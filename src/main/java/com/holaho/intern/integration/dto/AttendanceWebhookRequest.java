package com.holaho.intern.integration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceWebhookRequest {
    private String eventId;
    private String employeeId;
    private String eventType;
    private LocalDateTime eventTime;
    private String deviceId;
    private String method;
    private String rawPayload;
}
