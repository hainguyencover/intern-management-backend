package com.example.backend.dto.response;

import com.example.backend.enums.TicketCategory;
import com.example.backend.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SupportTicketResponse {
    private Long id;
    private String title;
    private String content;
    private TicketCategory category;
    private TicketStatus status;

    private Long createdByUserId;
    private String createdByEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
