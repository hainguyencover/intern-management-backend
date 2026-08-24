package com.holaho.intern.controller;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.entity.TicketComment;
import com.holaho.intern.service.SupportTicketService;
import com.holaho.intern.shared.dto.request.*;
import com.holaho.intern.shared.dto.response.*;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import com.holaho.intern.shared.enums.TicketStatus;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        SupportTicketResponse response = supportTicketService.getTicketDetail(ticket.getId(), user.getId(), false);
        return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu hỗ trợ thành công", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> getMyTickets(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<SupportTicket> page = supportTicketService.getMyTickets(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(SupportTicketResponse::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN', 'MENTOR')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isHrOrAdmin = isHrOrAdminUser(user);
        SupportTicketResponse response = supportTicketService.getTicketDetail(id, user.getId(), isHrOrAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> searchTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<SupportTicket> page = supportTicketService.searchTicketsEnhanced(
                status, category, priority, createdById, assignedToId, overdue, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(SupportTicketResponse::from)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        SupportTicket ticket = supportTicketService.assignTicket(id, request.getAssignedToId(), user.getId());
        SupportTicketResponse response = supportTicketService.getTicketDetail(ticket.getId(), user.getId(), true);
        return ResponseEntity.ok(ApiResponse.success("Đã assign ticket cho HR thành công", response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TicketStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        SupportTicket ticket = supportTicketService.updateTicketStatus(id, request.getStatus(), user.getId(), "Cập nhật trạng thái");
        SupportTicketResponse response = supportTicketService.getTicketDetail(ticket.getId(), user.getId(), true);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái ticket thành công", response));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> resolveTicket(
            @PathVariable Long id,
            @Valid @RequestBody TicketResolveRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        SupportTicket ticket = supportTicketService.resolveTicket(id, request.getResolution(), user.getId());
        SupportTicketResponse response = supportTicketService.getTicketDetail(ticket.getId(), user.getId(), true);
        return ResponseEntity.ok(ApiResponse.success("Đã giải quyết yêu cầu hỗ trợ", response));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> closeTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isHrOrAdmin = isHrOrAdminUser(user);
        SupportTicket ticket = supportTicketService.closeTicket(id, user.getId(), "Xác nhận hoàn tất ticket");
        SupportTicketResponse response = supportTicketService.getTicketDetail(ticket.getId(), user.getId(), isHrOrAdmin);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn tất đóng yêu cầu hỗ trợ", response));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN', 'MENTOR')")
    public ResponseEntity<ApiResponse<TicketCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody TicketCommentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isHrOrAdmin = isHrOrAdminUser(user);
        TicketComment comment = supportTicketService.addComment(id, user.getId(), request.getContent(), request.isInternal(), isHrOrAdmin);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm phản hồi", TicketCommentResponse.from(comment)));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        boolean isHrOrAdmin = isHrOrAdminUser(user);
        List<TicketCommentResponse> comments = supportTicketService.getCommentsByTicketId(id, user.getId(), isHrOrAdmin);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<TicketAttachmentResponse>> addAttachment(
            @PathVariable Long id,
            @RequestParam(required = false) Long commentId,
            @RequestParam String fileName,
            @RequestParam String originalFileName,
            @RequestParam String storageKey,
            @RequestParam String contentType,
            @RequestParam Long fileSize,
            @AuthenticationPrincipal CustomUserDetails user) {
        TicketAttachmentResponse attachment = supportTicketService.addAttachment(
                id, commentId, user.getId(), fileName, originalFileName, storageKey, contentType, fileSize);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm file đính kèm", attachment));
    }

    private boolean isHrOrAdminUser(CustomUserDetails user) {
        if (user == null || user.getAuthorities() == null) return false;
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
