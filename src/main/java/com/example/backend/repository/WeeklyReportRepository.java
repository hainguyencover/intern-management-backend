package com.example.backend.repository;

import com.example.backend.entity.WeeklyReport;
import com.example.backend.enums.GroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

        Optional<WeeklyReport> findByIntern_IdAndWeekNumber(Long internId, Integer weekNumber);

        boolean existsByIntern_IdAndWeekNumber(Long internId, Integer weekNumber);

        List<WeeklyReport> findByIntern_IdOrderByWeekNumberDesc(Long internId);

        Page<WeeklyReport> findByIntern_Id(Long internId, Pageable pageable);

        List<WeeklyReport> findByMentor_IdOrderByWeekNumberDesc(Long mentorId);

        Page<WeeklyReport> findByMentor_Id(Long mentorId, Pageable pageable);

        // FIX: Find reports by Intern's current mentor (User ID)
        Page<WeeklyReport> findByIntern_Mentor_User_Id(Long mentorUserId, Pageable pageable);

        @Query("SELECT r FROM WeeklyReport r WHERE " +
                        "(r.intern.mentor.id = :mentorId OR " +
                        "r.intern.id IN (SELECT gm.intern.id FROM GroupMember gm WHERE gm.group.mentorId = :mentorId AND gm.group.status = :groupStatus)) "
                        +
                        "AND (:internId IS NULL OR r.intern.id = :internId) " +
                        "AND (:status IS NULL OR r.status = :status)")
        Page<WeeklyReport> findReportsForMentor(
                        @Param("mentorId") Long mentorId,
                        @Param("groupStatus") GroupStatus groupStatus,
                        @Param("internId") Long internId,
                        @Param("status") String status,
                        Pageable pageable);

        List<WeeklyReport> findByStatusOrderByWeekNumberDesc(String status);

        Page<WeeklyReport> findByStatus(String status, Pageable pageable);

        long countByMentor_IdAndStatus(Long mentorId, String status);

        long countByIntern_Id(Long internId);

        List<WeeklyReport> findTop5ByMentor_IdOrderByWeekNumberDesc(Long mentorId);
}
