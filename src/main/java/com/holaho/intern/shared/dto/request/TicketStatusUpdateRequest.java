package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.TicketStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TicketStatusUpdateRequest {
    @NotNull
    private TicketStatus status;
}

