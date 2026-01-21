package com.example.backend.dto.request;

import com.example.backend.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SupportTicketCreateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private TicketCategory category; // optional
}
