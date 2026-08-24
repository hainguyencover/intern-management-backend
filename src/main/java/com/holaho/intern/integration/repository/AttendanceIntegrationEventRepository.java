package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.AttendanceIntegrationEvent;
import com.holaho.intern.integration.enums.EventProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceIntegrationEventRepository extends JpaRepository<AttendanceIntegrationEvent, Long> {

    Optional<AttendanceIntegrationEvent> findByTenantIdAndConnectionIdAndExternalEventId(
        Long tenantId, Long connectionId, String externalEventId
    );

    List<AttendanceIntegrationEvent> findByProcessingStatus(EventProcessingStatus status);
}
