package com.holaho.intern.mentor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorWorkloadSummaryResponse {

    private long totalMentors;
    private long mentorsWithInterns;
    private long totalActiveInterns;
    private double averageInternsPerMentor;
    private long noAssignmentCount;
    private long underloadCount;
    private long normalCount;
    private long nearCapacityCount;
    private long overloadCount;
}
