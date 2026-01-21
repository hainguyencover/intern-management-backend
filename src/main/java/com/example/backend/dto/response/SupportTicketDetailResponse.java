package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SupportTicketDetailResponse {
    private SupportTicketResponse ticket;
    private List<TicketCommentResponse> comments;
}
