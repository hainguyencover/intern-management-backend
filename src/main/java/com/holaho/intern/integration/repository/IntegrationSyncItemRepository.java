package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.IntegrationSyncItem;
import com.holaho.intern.integration.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationSyncItemRepository extends JpaRepository<IntegrationSyncItem, Long> {

    List<IntegrationSyncItem> findBySyncJobId(Long syncJobId);

    List<IntegrationSyncItem> findBySyncJobIdAndStatus(Long syncJobId, SyncStatus status);
}
