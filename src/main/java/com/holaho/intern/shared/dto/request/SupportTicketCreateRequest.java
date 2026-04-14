package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.TicketCategory;
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

