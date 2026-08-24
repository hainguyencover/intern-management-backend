package com.holaho.intern.reporting.repository;

import com.holaho.intern.reporting.entity.ExportJob;
import com.holaho.intern.reporting.enums.ExportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportJobRepository extends JpaRepository<ExportJob, Long> {

    Optional<ExportJob> findByTenantIdAndJobCode(Long tenantId, String jobCode);

    Page<ExportJob> findByTenantIdAndRequestedByOrderByCreatedAtDesc(Long tenantId, Long requestedBy, Pageable pageable);

    Page<ExportJob> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<ExportJob> findByStatusAndExpiresAtBefore(ExportJobStatus status, LocalDateTime now);
}
