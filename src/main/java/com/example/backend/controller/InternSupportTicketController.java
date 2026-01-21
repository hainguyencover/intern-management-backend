package com.example.backend.controller;

import com.example.backend.dto.request.SupportTicketCreateRequest;
import com.example.backend.dto.request.TicketCommentCreateRequest;
import com.example.backend.dto.response.SupportTicketDetailResponse;
import com.example.backend.dto.response.SupportTicketResponse;
import com.example.backend.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/intern/support-tickets")
@PreAuthorize("hasRole('INTERN')")
public class InternSupportTicketController {

    private final SupportTicketService service;

    public InternSupportTicketController(SupportTicketService service) {
        this.service = service;
    }

    @PostMapping
    public SupportTicketResponse create(@Valid @RequestBody SupportTicketCreateRequest req) {
        return service.createTicket(req);
    }

    @GetMapping
    public Page<SupportTicketResponse> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getMyTickets(page, size);
    }

    @GetMapping("/{id}")
    public SupportTicketDetailResponse detail(@PathVariable Long id) {
        return service.getMyTicketDetail(id);
    }

    @PostMapping("/{id}/comments")
    public void addComment(@PathVariable Long id, @Valid @RequestBody TicketCommentCreateRequest req) {
        service.addMyComment(id, req);
    }
}
