package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MentorCreateRequest {

    @NotNull
    private Long userId;

    private Long departmentId;

    private String title;
}
