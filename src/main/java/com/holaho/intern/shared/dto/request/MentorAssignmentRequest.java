package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MentorAssignmentRequest {

    private Long enrollmentId;

    @NotNull(message = "internId is required")
    private Long internId;

    @NotNull(message = "mentorId is required")
    private Long mentorId;

    private String reason;
}
