package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketAssignRequest {
    @NotNull(message = "Assigned user ID is required")
    private Long assignedToId;
}
