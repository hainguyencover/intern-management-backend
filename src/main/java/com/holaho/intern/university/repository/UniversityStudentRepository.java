package com.holaho.intern.university.repository;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.university.repository.projection.UniversityStudentProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityStudentRepository extends JpaRepository<InternProfile, Long> {

    @Query("SELECT ip FROM InternProfile ip WHERE ip.id = :studentId AND (ip.universityEntity.id = :universityId OR ip.universityId = :universityId)")
    Optional<InternProfile> findByIdAndUniversityId(@Param("studentId") Long studentId, @Param("universityId") Long universityId);

    @Query(value = """
        SELECT 
            i.id AS id,
            i.student_code AS studentCode,
            u.full_name AS fullName,
            i.major AS major,
            i.status AS status,
            m_user.full_name AS mentorName,
            p.name AS programName,
            COALESCE(t_summary.total_tasks, 0) AS totalTasks,
            COALESCE(t_summary.completed_tasks, 0) AS completedTasks,
            COALESCE(t_summary.overdue_tasks, 0) AS overdueTasks,
            COALESCE(att_summary.working_days, 0) AS workingDays,
            COALESCE(att_summary.present_days, 0) AS presentDays,
            COALESCE(att_summary.leave_days, 0) AS leaveDays,
            CASE 
                WHEN COALESCE(att_summary.working_days, 0) > 0 
                THEN ROUND((COALESCE(att_summary.present_days, 0) * 100.0) / att_summary.working_days, 2)
                ELSE 100.00
            END AS attendanceRate,
            ev.overall_score AS overallScore,
            ev.status AS evaluationStatus
        FROM intern_profiles i
        JOIN users u ON u.id = i.user_id
        LEFT JOIN mentors m ON m.id = i.mentor_id
        LEFT JOIN users m_user ON m_user.id = m.user_id
        LEFT JOIN (
            SELECT ie.intern_id, ie.program_id
            FROM internship_enrollments ie
            WHERE ie.status = 'ACTIVE'
            LIMIT 1
        ) active_enr ON active_enr.intern_id = i.id
        LEFT JOIN programs p ON p.id = active_enr.program_id
        LEFT JOIN (
            SELECT 
                assignee_id,
                COUNT(id) AS total_tasks,
                SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks,
                SUM(CASE WHEN due_date < CURRENT_DATE AND status != 'COMPLETED' THEN 1 ELSE 0 END) AS overdue_tasks
            FROM tasks
            GROUP BY assignee_id
        ) t_summary ON t_summary.assignee_id = i.user_id
        LEFT JOIN (
            SELECT 
                intern_profile_id,
                COUNT(id) AS working_days,
                SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days,
                SUM(CASE WHEN status IN ('LEAVE_APPROVED', 'EXCUSED') THEN 1 ELSE 0 END) AS leave_days
            FROM attendance_records
            GROUP BY intern_profile_id
        ) att_summary ON att_summary.intern_profile_id = i.id
        LEFT JOIN evaluations ev ON ev.intern_profile_id = i.id
        WHERE (i.university_id = :universityId)
          AND (:keyword IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.student_code) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR i.status = :status)
          AND (:major IS NULL OR i.major = :major)
    """, 
    countQuery = """
        SELECT COUNT(i.id)
        FROM intern_profiles i
        JOIN users u ON u.id = i.user_id
        WHERE (i.university_id = :universityId)
          AND (:keyword IS NULL OR LOWER(u.full_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.student_code) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR i.status = :status)
          AND (:major IS NULL OR i.major = :major)
    """,
    nativeQuery = true)
    Page<UniversityStudentProjection> searchStudents(
        @Param("universityId") Long universityId,
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("major") String major,
        Pageable pageable
    );

    @Query("SELECT COUNT(ip) FROM InternProfile ip WHERE ip.universityEntity.id = :universityId OR ip.universityId = :universityId")
    long countTotalStudents(@Param("universityId") Long universityId);

    @Query("SELECT COUNT(ip) FROM InternProfile ip WHERE (ip.universityEntity.id = :universityId OR ip.universityId = :universityId) AND ip.status = :status")
    long countStudentsByStatus(@Param("universityId") Long universityId, @Param("status") String status);

    @Query("SELECT DISTINCT ip.major FROM InternProfile ip WHERE (ip.universityEntity.id = :universityId OR ip.universityId = :universityId) AND ip.major IS NOT NULL")
    List<String> findDistinctMajorsByUniversityId(@Param("universityId") Long universityId);
}
