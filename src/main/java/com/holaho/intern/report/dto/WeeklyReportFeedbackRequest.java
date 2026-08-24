package com.holaho.intern.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportFeedbackRequest {

    @NotBlank(message = "Feedback content is required")
    @Size(max = 5000, message = "Feedback content must not exceed 5000 characters")
    private String content;
}
