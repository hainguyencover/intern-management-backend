package com.holaho.intern.mentor.repository;

import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Long> {

    Optional<MentorAssignment> findByTenantIdAndId(Long tenantId, Long id);

    long countByTenantIdAndMentorIdAndStatus(Long tenantId, Long mentorId, MentorAssignmentStatus status);

    List<MentorAssignment> findByTenantIdAndMentorIdAndStatus(Long tenantId, Long mentorId, MentorAssignmentStatus status);

    Optional<MentorAssignment> findByTenantIdAndInternIdAndStatus(Long tenantId, Long internId, MentorAssignmentStatus status);

    Optional<MentorAssignment> findByTenantIdAndEnrollmentIdAndStatus(Long tenantId, Long enrollmentId, MentorAssignmentStatus status);

    List<MentorAssignment> findByTenantIdAndInternIdOrderByStartDateDescAssignedAtDesc(Long tenantId, Long internId);

    List<MentorAssignment> findByTenantIdAndMentorIdOrderByStartDateDescAssignedAtDesc(Long tenantId, Long mentorId);

    @Query("""
        SELECT a FROM MentorAssignment a
        WHERE a.tenantId = :tenantId
          AND a.intern.id = :internId
          AND a.status = :status
          AND a.assignmentType = :assignmentType
    """)
    List<MentorAssignment> findActivePrimaryAssignmentsForIntern(
            @Param("tenantId") Long tenantId,
            @Param("internId") Long internId,
            @Param("status") MentorAssignmentStatus status,
            @Param("assignmentType") MentorAssignmentType assignmentType
    );

    @Query("""
        SELECT a FROM MentorAssignment a
        JOIN FETCH a.mentor m
        JOIN FETCH a.intern i
        LEFT JOIN FETCH i.user u
        WHERE a.tenantId = :tenantId
          AND a.mentor.id = :mentorId
          AND a.status = 'ACTIVE'
    """)
    List<MentorAssignment> findActiveAssignmentsWithDetailsByMentorId(
            @Param("tenantId") Long tenantId,
            @Param("mentorId") Long mentorId
    );

    @Query("""
        SELECT a.mentor.id AS mentorId, COUNT(a.id) AS activeInternCount
        FROM MentorAssignment a
        WHERE a.tenantId = :tenantId
          AND a.status = com.holaho.intern.shared.enums.MentorAssignmentStatus.ACTIVE
        GROUP BY a.mentor.id
    """)
    List<com.holaho.intern.mentor.dto.MentorWorkloadProjection> countActiveWorkloadsByTenantId(@Param("tenantId") Long tenantId);

    long countByMentorIdAndStatus(Long mentorId, MentorAssignmentStatus status);

    List<MentorAssignment> findByMentorIdAndStatus(Long mentorId, MentorAssignmentStatus status);

    Optional<MentorAssignment> findByInternIdAndStatus(Long internId, MentorAssignmentStatus status);

    List<MentorAssignment> findByInternIdOrderByAssignedAtDesc(Long internId);
}

