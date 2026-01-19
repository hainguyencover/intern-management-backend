package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {
    private Long id;
    private Long internId;
    private String internName;
    private Long mentorId;
    private String mentorName;
    private String period;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
