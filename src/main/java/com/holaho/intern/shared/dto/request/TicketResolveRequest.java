package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketResolveRequest {
    @NotBlank(message = "Resolution text is required when resolving a ticket")
    @Size(min = 5, max = 2000, message = "Resolution must be between 5 and 2000 characters")
    private String resolution;
}
