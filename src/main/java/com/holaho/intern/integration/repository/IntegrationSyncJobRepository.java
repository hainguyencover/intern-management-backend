package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.IntegrationSyncJob;
import com.holaho.intern.integration.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationSyncJobRepository extends JpaRepository<IntegrationSyncJob, Long> {

    List<IntegrationSyncJob> findByTenantId(Long tenantId);

    List<IntegrationSyncJob> findByConnectionIdOrderByCreatedAtDesc(Long connectionId);

    List<IntegrationSyncJob> findByConnectionIdAndStatus(Long connectionId, SyncStatus status);
}
