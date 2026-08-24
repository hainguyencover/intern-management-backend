package com.holaho.intern.evaluation.repository;

import com.holaho.intern.evaluation.entity.FinalEvaluationReport;
import com.holaho.intern.evaluation.enums.FinalReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalEvaluationReportRepository extends JpaRepository<FinalEvaluationReport, Long> {

    Optional<FinalEvaluationReport> findByInternIdAndTenantId(Long internId, Long tenantId);

    boolean existsByInternIdAndTenantId(Long internId, Long tenantId);

    @Query("SELECT r FROM FinalEvaluationReport r WHERE r.tenantId = :tenantId " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(r.intern.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.intern.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.createdAt DESC")
    Page<FinalEvaluationReport> findAllWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") FinalReportStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);
}
