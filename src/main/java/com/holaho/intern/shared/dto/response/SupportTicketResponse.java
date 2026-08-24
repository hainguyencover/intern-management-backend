package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.shared.enums.SlaStatus;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import com.holaho.intern.shared.enums.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketResponse {
    private Long id;
    private String ticketCode;
    private Long createdById;
    private String creatorName;
    private Long assignedToId;
    private String assignedToName;
    private TicketCategory category;
    private TicketPriority priority;
    private String title;
    private String content;
    private String resolution;
    private TicketStatus status;
    private SlaStatus slaStatus;
    private LocalDateTime firstResponseAt;
    private LocalDateTime firstResponseDueAt;
    private LocalDateTime resolutionDueAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Long closedById;
    private String closedByName;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketAttachmentResponse> attachments;
    private List<TicketStatusHistoryResponse> statusHistory;

    public static SupportTicketResponse from(SupportTicket ticket) {
        return from(ticket, null, null);
    }

    public static SupportTicketResponse from(SupportTicket ticket, List<TicketAttachmentResponse> attachments, List<TicketStatusHistoryResponse> history) {
        if (ticket == null) return null;

        SlaStatus sla = SlaStatus.ON_TIME;
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getFirstResponseAt() == null && ticket.getFirstResponseDueAt() != null) {
            if (now.isAfter(ticket.getFirstResponseDueAt())) {
                sla = SlaStatus.OVERDUE;
            } else if (now.plusHours(12).isAfter(ticket.getFirstResponseDueAt())) {
                sla = SlaStatus.AT_RISK;
            }
        }

        return SupportTicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .createdById(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getId() : null)
                .creatorName(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getFullName() : null)
                .assignedToId(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null)
                .assignedToName(ticket.getAssignedTo() != null ? ticket.getAssignedTo().getFullName() : null)
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .resolution(ticket.getResolution())
                .status(ticket.getStatus())
                .slaStatus(sla)
                .firstResponseAt(ticket.getFirstResponseAt())
                .firstResponseDueAt(ticket.getFirstResponseDueAt())
                .resolutionDueAt(ticket.getResolutionDueAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .closedById(ticket.getClosedBy() != null ? ticket.getClosedBy().getId() : null)
                .closedByName(ticket.getClosedBy() != null ? ticket.getClosedBy().getFullName() : null)
                .version(ticket.getVersion())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .attachments(attachments)
                .statusHistory(history)
                .build();
    }
}
