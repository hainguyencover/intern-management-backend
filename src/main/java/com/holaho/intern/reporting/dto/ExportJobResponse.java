package com.holaho.intern.reporting.dto;

import com.holaho.intern.reporting.enums.ExportFormat;
import com.holaho.intern.reporting.enums.ExportJobStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobResponse {
    private String jobId;
    private String reportCode;
    private ExportFormat format;
    private ExportJobStatus status;
    private String fileName;
    private Long fileSize;
    private Integer recordCount;
    private String errorMessage;
    private Long requestedBy;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String downloadUrl;
}
