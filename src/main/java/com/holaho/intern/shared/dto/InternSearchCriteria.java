package com.holaho.intern.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternSearchCriteria {
    private String university;
    private String major;
    private Double minGpa;
    private Double maxGpa;
    private String status;
    private String keyword; // Search in name, email, student code
    private Long mentorId;
    private Boolean excludeBusy; // New field
    private String sortBy;
    private String sortDirection;
}

