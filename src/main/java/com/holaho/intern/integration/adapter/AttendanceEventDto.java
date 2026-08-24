package com.holaho.intern.integration.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEventDto {
    private String externalEventId;
    private String externalEmployeeId;
    private String eventType;
    private LocalDateTime eventTime;
    private String deviceId;
    private String method;
    private String rawPayload;
}
