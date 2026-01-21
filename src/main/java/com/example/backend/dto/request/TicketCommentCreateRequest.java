package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TicketCommentCreateRequest {
    @NotBlank
    private String content;
}
