package com.example.backend.dto.response;

import lombok.*;

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

    private Integer skillScore;
    private Integer attitudeScore;
    private Integer overallScore;

    private String comment;
}
