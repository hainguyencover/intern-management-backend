package com.example.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignInternRequest {

    @NotNull(message = "Intern ID is required")
    private Long internId;
}
