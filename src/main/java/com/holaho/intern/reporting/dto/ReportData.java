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
public class ReportData {
    private String reportCode;
    private String reportName;
    private String category;
    private String filterSummary;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private Map<String, Object> summary;
    private String generatedBy;
    private LocalDateTime generatedAt;

    public int getRecordCount() {
        return rows != null ? rows.size() : 0;
    }
}
