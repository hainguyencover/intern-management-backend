package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCapacityResponse {
    private Long programId;
    private String programName;
    private String programCode;
    private long currentInterns;
    private Integer maxInterns;
    private Integer availableSlots;
    private boolean full;
    private boolean nearCapacity;
    private double capacityPercentage;
}
