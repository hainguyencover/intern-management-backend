package com.holaho.intern.report.repository;

import com.holaho.intern.report.entity.WeeklyReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository("reportWeeklyReportRepository")
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long>, JpaSpecificationExecutor<WeeklyReport> {

    boolean existsByTenantIdAndInternIdAndWeekStartDate(Long tenantId, Long internId, LocalDate weekStartDate);

    Optional<WeeklyReport> findByIdAndTenantId(Long id, Long tenantId);

    Optional<WeeklyReport> findByIdAndTenantIdAndInternId(Long id, Long tenantId, Long internId);

    Optional<WeeklyReport> findByIdAndTenantIdAndMentorId(Long id, Long tenantId, Long mentorId);

    Page<WeeklyReport> findByTenantIdAndInternId(Long tenantId, Long internId, Pageable pageable);

    Page<WeeklyReport> findByTenantIdAndMentorId(Long tenantId, Long mentorId, Pageable pageable);

    long countByTenantIdAndInternId(Long tenantId, Long internId);
}
