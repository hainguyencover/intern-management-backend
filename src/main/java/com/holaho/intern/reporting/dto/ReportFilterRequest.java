package com.holaho.intern.reporting.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportFilterRequest {
    private Long programId;
    private Long departmentId;
    private Long universityId;
    private Long majorId;
    private Long mentorId;
    private String internStatus;
    private String internshipPeriod;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Map<String, Object> customFilters;
}
