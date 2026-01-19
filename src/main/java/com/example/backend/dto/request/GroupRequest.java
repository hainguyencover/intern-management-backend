package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {

    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotBlank(message = "Group name is required")
    private String name;

    private Long departmentId;
    private Long mentorId;

    private java.time.LocalTime workStartTime;
    private java.time.LocalTime workEndTime;
    private String workDays;
}
