package com.holaho.intern.reporting.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportPreviewResponse {
    private String reportCode;
    private String reportName;
    private int totalRecords;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Map<String, Object> summary;
    private LocalDateTime generatedAt;
}
