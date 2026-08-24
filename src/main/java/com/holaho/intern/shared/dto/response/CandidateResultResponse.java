package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResultResponse {
    private Long applicationId;
    private String status;
    private String programName;
    private String position;
    private String reviewerName;
    private String reviewComment;
    private LocalDateTime decidedAt;
    private String nextStep;
}
