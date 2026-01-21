package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpsertEvaluationRequest {

    @NotNull
    private Long internId;

    @NotBlank
    @Size(max = 50)
    private String period; // ví dụ: "2026-Q1"

    @NotNull
    @Min(0) @Max(10)
    private Integer skillScore;

    @NotNull
    @Min(0) @Max(10)
    private Integer attitudeScore;

    @Size(max = 2000)
    private String comment;

    @Min(0) @Max(10)
    private Integer overallScore; // optional; nếu null thì BE tự tính
}
