package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignInternsRequest {

    @NotNull(message = "Intern IDs are required")
    private List<Long> internIds;
}
