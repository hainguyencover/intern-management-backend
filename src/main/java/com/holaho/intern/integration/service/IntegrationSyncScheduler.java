package com.holaho.intern.integration.service;

import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationSyncJob;
import com.holaho.intern.integration.enums.IntegrationType;
import com.holaho.intern.integration.repository.IntegrationConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationSyncScheduler {

    private final IntegrationConnectionRepository connectionRepository;
    private final HrmSyncService hrmSyncService;

    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void runAutomatedHrmSync() {
        log.info("Starting scheduled HRM automated synchronization...");
        List<IntegrationConnection> activeHrmConnections = connectionRepository
                .findByEnabledTrueAndIntegrationType(IntegrationType.HRM);

        for (IntegrationConnection connection : activeHrmConnections) {
            try {
                log.info("Triggering scheduled sync for connection: {}", connection.getCode());
                IntegrationSyncJob job = hrmSyncService.startHrmSync(connection.getId());
                hrmSyncService.executeHrmSync(job.getId());
            } catch (Exception e) {
                log.error("Error running scheduled sync for connection {}: {}", connection.getCode(), e.getMessage());
            }
        }
    }
}
