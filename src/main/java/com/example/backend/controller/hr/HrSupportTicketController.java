package com.example.backend.controller.hr;

import com.example.backend.dto.request.SupportTicketStatusUpdateRequest;
import com.example.backend.dto.request.TicketCommentCreateRequest;
import com.example.backend.dto.response.SupportTicketDetailResponse;
import com.example.backend.dto.response.SupportTicketResponse;
import com.example.backend.service.SupportTicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/support-tickets")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class HrSupportTicketController {

    private final SupportTicketService service;

    public HrSupportTicketController(SupportTicketService service) {
        this.service = service;
    }

    @GetMapping
    public Page<SupportTicketResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return service.getAllTickets(page, size);
    }

    @GetMapping("/{id}")
    public SupportTicketDetailResponse detail(@PathVariable Long id) {
        return service.getTicketDetailForHr(id);
    }

    @PostMapping("/{id}/comments")
    public void addComment(@PathVariable Long id, @Valid @RequestBody TicketCommentCreateRequest req) {
        service.addHrComment(id, req);
    }

    @PatchMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id, @Valid @RequestBody SupportTicketStatusUpdateRequest req) {
        service.updateStatus(id, req);
    }
}
