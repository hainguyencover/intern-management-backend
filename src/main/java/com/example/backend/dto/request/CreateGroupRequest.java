package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGroupRequest {
    @NotNull(message = "mentorId must not be null")
    private Long mentorId;

    private String name; // optional
}
