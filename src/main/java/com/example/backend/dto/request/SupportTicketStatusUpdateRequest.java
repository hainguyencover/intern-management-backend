package com.example.backend.dto.request;

import com.example.backend.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class  SupportTicketStatusUpdateRequest {
    @NotNull
    private TicketStatus status;
}
