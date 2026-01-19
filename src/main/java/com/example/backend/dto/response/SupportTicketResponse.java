package com.example.backend.dto.response;

import com.example.backend.entity.SupportTicket;
import com.example.backend.enums.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketResponse {
    private Long id;
    private String creatorName;
    private TicketCategory category;
    private String title;
    private String content;
    private TicketStatus status;
    private LocalDateTime createdAt;

    public static SupportTicketResponse from(SupportTicket ticket) {
        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .creatorName(ticket.getCreatedBy().getFullName())
                .category(ticket.getCategory())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
