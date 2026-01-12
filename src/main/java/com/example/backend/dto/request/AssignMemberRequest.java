package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignMemberRequest {
    @NotNull(message = "internId must not be null")
    private Long internId;
}
