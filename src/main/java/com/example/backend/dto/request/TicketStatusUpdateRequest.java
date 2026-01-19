package com.example.backend.dto.request;

import com.example.backend.enums.TicketStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TicketStatusUpdateRequest {
    @NotNull
    private TicketStatus status;
}
