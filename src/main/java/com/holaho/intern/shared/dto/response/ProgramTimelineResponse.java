package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.ProgramStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgramTimelineResponse {

    private Long programId;
    private String programName;
    private String programCode;
    private Long departmentId;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long durationDays;
    private ProgramStatus status;
    private Long internCount;
    private Boolean warningNearingEnd;
}
