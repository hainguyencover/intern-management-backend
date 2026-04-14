package com.holaho.intern.shared.dto.request;

import com.holaho.intern.task.entity.Task;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotBlank(message = "Task title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String description;

    private LocalDateTime dueDate;

    private Long assigneeId;
}

