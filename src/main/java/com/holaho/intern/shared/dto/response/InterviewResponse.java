package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {
    private Long id;
    private Long applicationId;
    private LocalDateTime scheduledTime;
    private String location;
    private Long interviewerId;
    private String interviewerName;
    private String status;
    private String feedback;
}
