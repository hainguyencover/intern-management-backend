package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TicketCommentRequest {
    @NotBlank
    @Size(max = 4000)
    private String content;
}

