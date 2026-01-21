package com.example.backend.service;

import com.example.backend.dto.request.SupportTicketCreateRequest;
import com.example.backend.dto.request.SupportTicketStatusUpdateRequest;
import com.example.backend.dto.request.TicketCommentCreateRequest;
import com.example.backend.dto.response.SupportTicketDetailResponse;
import com.example.backend.dto.response.SupportTicketResponse;
import org.springframework.data.domain.Page;

public interface SupportTicketService {
    // Intern
    SupportTicketResponse createTicket(SupportTicketCreateRequest req);
    Page<SupportTicketResponse> getMyTickets(int page, int size);
    SupportTicketDetailResponse getMyTicketDetail(Long ticketId);
    void addMyComment(Long ticketId, TicketCommentCreateRequest req);

    // HR
    Page<SupportTicketResponse> getAllTickets(Integer page, Integer size);
    SupportTicketDetailResponse getTicketDetailForHr(Long ticketId);
    void addHrComment(Long ticketId, TicketCommentCreateRequest req);
    void updateStatus(Long ticketId, SupportTicketStatusUpdateRequest req);
}
