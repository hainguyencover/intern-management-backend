package com.holaho.intern.repository;

import com.holaho.intern.entity.InternshipEnrollment;
import com.holaho.intern.shared.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternshipEnrollmentRepository extends JpaRepository<InternshipEnrollment, Long> {

    List<InternshipEnrollment> findByProgramId(Long programId);

    List<InternshipEnrollment> findByInternId(Long internId);

    Optional<InternshipEnrollment> findByProgramIdAndInternId(Long programId, Long internId);

    Optional<InternshipEnrollment> findByInternIdAndStatus(Long internId, EnrollmentStatus status);

    @Query("SELECT e FROM InternshipEnrollment e WHERE e.intern.id = :internId AND e.status = 'ACTIVE'")
    Optional<InternshipEnrollment> findActiveEnrollmentByInternId(@Param("internId") Long internId);

    boolean existsByProgramIdAndInternId(Long programId, Long internId);
}
