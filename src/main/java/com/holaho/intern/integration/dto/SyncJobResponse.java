package com.holaho.intern.integration.dto;

import com.holaho.intern.integration.enums.SyncDirection;
import com.holaho.intern.integration.enums.SyncStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SyncJobResponse {
    private Long id;
    private Long connectionId;
    private String connectionCode;
    private String syncType;
    private SyncDirection direction;
    private SyncStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer totalRecords;
    private Integer successRecords;
    private Integer failedRecords;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
