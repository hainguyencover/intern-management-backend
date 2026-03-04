package com.example.backend.controller;

import com.example.backend.dto.request.SupportTicketCreateRequest;
import com.example.backend.dto.request.TicketCommentRequest;
import com.example.backend.dto.request.TicketStatusUpdateRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.SupportTicketResponse;
import com.example.backend.dto.response.TicketCommentResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.*;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody SupportTicketCreateRequest request) {
        SupportTicket ticket = supportTicketService.createTicket(request, user.getId());
        return ResponseEntity
                .ok(ApiResponse.success("Ticket created successfully", SupportTicketResponse.from(ticket)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<SupportTicket> page = supportTicketService.getMyTickets(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(SupportTicketResponse::from)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<SupportTicket> page = supportTicketService.searchTickets(
                status, category, createdById, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(SupportTicketResponse::from)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        SupportTicket ticket = supportTicketService.updateTicketStatus(id, request.getStatus());
        return ResponseEntity
                .ok(ApiResponse.success("Ticket status updated successfully", SupportTicketResponse.from(ticket)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TicketCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        TicketComment comment = supportTicketService.addComment(id, user.getId(), request.getContent());
        return ResponseEntity
                .ok(ApiResponse.success("Comment added successfully", TicketCommentResponse.from(comment)));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(@PathVariable Long id) {
        List<TicketComment> comments = supportTicketService.getCommentsByTicketId(id);
        List<TicketCommentResponse> response = comments.stream().map(TicketCommentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
