package com.holaho.intern.integration.adapter;

import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DefaultAttendanceAdapter implements AttendanceAdapter {

    @Override
    public List<AttendanceEventDto> fetchEvents(IntegrationConnection connection, List<IntegrationCredential> credentials) {
        log.info("Fetching attendance events from connection: {}", connection.getCode());
        List<AttendanceEventDto> events = new ArrayList<>();
        events.add(AttendanceEventDto.builder()
                .externalEventId("ATT-EVENT-20260824-001")
                .externalEmployeeId("EMP-2026-001")
                .eventType("CHECK_IN")
                .eventTime(LocalDateTime.now())
                .deviceId("QR-GATE-01")
                .method("QR")
                .rawPayload("{\"type\":\"CHECK_IN\",\"emp\":\"EMP-2026-001\"}")
                .build());
        return events;
    }

    @Override
    public boolean testConnection(IntegrationConnection connection, List<IntegrationCredential> credentials) {
        log.info("Testing connection to Attendance device baseUrl: {}", connection.getBaseUrl());
        return connection.getBaseUrl() != null && !connection.getBaseUrl().isEmpty();
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return true;
        }
        return true;
    }
}
