package com.example.backend.dto.request;

import com.example.backend.enums.TicketCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SupportTicketCreateRequest {
    @NotNull
    private TicketCategory category;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String content;
}
