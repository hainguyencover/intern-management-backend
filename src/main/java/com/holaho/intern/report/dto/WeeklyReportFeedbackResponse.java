package com.holaho.intern.report.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportFeedbackResponse {

    private Long id;
    private Long mentorId;
    private String mentorName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
