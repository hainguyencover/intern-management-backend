package com.holaho.intern.service;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.entity.TicketAttachment;
import com.holaho.intern.entity.TicketComment;
import com.holaho.intern.entity.TicketStatusHistory;
import com.holaho.intern.repository.SupportTicketRepository;
import com.holaho.intern.repository.TicketAttachmentRepository;
import com.holaho.intern.repository.TicketCommentRepository;
import com.holaho.intern.repository.TicketStatusHistoryRepository;
import com.holaho.intern.shared.dto.request.SupportTicketCreateRequest;
import com.holaho.intern.shared.dto.response.SupportTicketResponse;
import com.holaho.intern.shared.dto.response.TicketAttachmentResponse;
import com.holaho.intern.shared.dto.response.TicketCommentResponse;
import com.holaho.intern.shared.dto.response.TicketStatusHistoryResponse;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import com.holaho.intern.shared.enums.TicketStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.util.WorkingDayCalculator;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupportTicket createTicket(SupportTicketCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

        LocalDateTime now = LocalDateTime.now();

        SupportTicket ticket = new SupportTicket();
        ticket.setCreatedBy(user);
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);
        ticket.setTitle(request.getTitle());
        ticket.setContent(request.getContent());
        ticket.setStatus(TicketStatus.OPEN);

        // Code placeholder, saved first to get ID
        ticket.setTicketCode("SUP-" + Year.now().getValue() + "-TMP-" + System.currentTimeMillis());
        // Calculate SLA 3 working days according to BR-08 / US-028
        ticket.setFirstResponseDueAt(WorkingDayCalculator.calculateDueDate(now, 3));

        ticket = ticketRepository.save(ticket);

        // Set final readable ticket code
        String ticketCode = String.format("SUP-%d-%06d", Year.now().getValue(), ticket.getId());
        ticket.setTicketCode(ticketCode);
        ticket = ticketRepository.save(ticket);

        // Record status history
        recordStatusHistory(ticket, null, TicketStatus.OPEN, user, "Ticket created");

        log.info("Created support ticket {} (code: {}) by user: {}", ticket.getId(), ticketCode, userId);
        return ticket;
    }

    @Transactional
    public SupportTicket assignTicket(Long ticketId, Long assignedToId, Long actionByUserId) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User assignedTo = userRepository.findById(assignedToId)
                .orElseThrow(() -> new NotFoundException("HR user không tồn tại: " + assignedToId));

        User actionByUser = userRepository.findById(actionByUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + actionByUserId));

        ticket.setAssignedTo(assignedTo);

        // Auto move to IN_PROGRESS if OPEN
        if (ticket.getStatus() == TicketStatus.OPEN) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            recordStatusHistory(ticket, oldStatus, TicketStatus.IN_PROGRESS, actionByUser, "Assigned to " + assignedTo.getFullName());
        }

        log.info("Assigned ticket {} to HR user {}", ticketId, assignedToId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus newStatus, Long actionByUserId, String reason) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User actionByUser = userRepository.findById(actionByUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + actionByUserId));

        TicketStatus currentStatus = ticket.getStatus();
        if (currentStatus == newStatus) {
            return ticket;
        }

        // Validate state machine rules
        validateStateTransition(currentStatus, newStatus);

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
            ticket.setClosedBy(actionByUser);
        }

        recordStatusHistory(ticket, currentStatus, newStatus, actionByUser, reason);
        log.info("Updated ticket {} status from {} to {}", ticketId, currentStatus, newStatus);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket resolveTicket(Long ticketId, String resolution, Long actionByUserId) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User actionByUser = userRepository.findById(actionByUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + actionByUserId));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Không thể resolve ticket đã ở trạng thái CLOSED");
        }

        TicketStatus currentStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolution(resolution);
        ticket.setResolvedAt(LocalDateTime.now());

        recordStatusHistory(ticket, currentStatus, TicketStatus.RESOLVED, actionByUser, "Resolved with solution");
        log.info("Resolved ticket {} by user {}", ticketId, actionByUserId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket closeTicket(Long ticketId, Long actionByUserId, String reason) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User actionByUser = userRepository.findById(actionByUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + actionByUserId));

        TicketStatus currentStatus = ticket.getStatus();
        if (currentStatus == TicketStatus.CLOSED) {
            return ticket;
        }

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        ticket.setClosedBy(actionByUser);

        recordStatusHistory(ticket, currentStatus, TicketStatus.CLOSED, actionByUser, reason != null ? reason : "Closed ticket");
        log.info("Closed ticket {} by user {}", ticketId, actionByUserId);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, Long authorId, String content, boolean isInternal, boolean isHrOrAdmin) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + authorId));

        // Interns cannot post internal comments
        if (isInternal && !isHrOrAdmin) {
            throw new ForbiddenException("Chỉ HR/Admin mới có quyền tạo ghi chú nội bộ");
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content);
        comment.setInternal(isInternal);
        comment = commentRepository.save(comment);

        // Update firstResponseAt if this is the first HR reply
        if (isHrOrAdmin && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(LocalDateTime.now());
            if (ticket.getStatus() == TicketStatus.OPEN) {
                ticket.setStatus(TicketStatus.IN_PROGRESS);
                recordStatusHistory(ticket, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, author, "HR replied to ticket");
            }
            ticketRepository.save(ticket);
        }

        log.info("Added comment to ticket: {} (internal: {})", ticketId, isInternal);
        return comment;
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicketDetail(Long ticketId, Long currentUserId, boolean isHrOrAdmin) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        // Ownership enforcement for Interns
        if (!isHrOrAdmin && !ticket.getCreatedBy().getId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xem ticket của thực tập sinh khác");
        }

        List<TicketAttachment> attachments = attachmentRepository.findByTicketIdAndDeletedAtIsNull(ticketId);
        List<TicketStatusHistory> history = statusHistoryRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        List<TicketAttachmentResponse> attachmentResponses = attachments.stream().map(TicketAttachmentResponse::from).toList();
        List<TicketStatusHistoryResponse> historyResponses = history.stream().map(TicketStatusHistoryResponse::from).toList();

        return SupportTicketResponse.from(ticket, attachmentResponses, historyResponses);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> getMyTickets(Long userId, Pageable pageable) {
        return ticketRepository.findByCreatedByIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> searchTicketsEnhanced(
            TicketStatus status, TicketCategory category, TicketPriority priority,
            Long createdById, Long assignedToId, Boolean overdue, String keyword, Pageable pageable) {
        return ticketRepository.searchEnhanced(
                status, category, priority, createdById, assignedToId, overdue, LocalDateTime.now(), keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getCommentsByTicketId(Long ticketId, Long currentUserId, boolean isHrOrAdmin) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        if (!isHrOrAdmin && !ticket.getCreatedBy().getId().equals(currentUserId)) {
            throw new ForbiddenException("Bạn không có quyền xem trao đổi của ticket này");
        }

        List<TicketComment> comments = commentRepository.findByTicketIdWithAuthor(ticketId);
        return comments.stream()
                .filter(c -> isHrOrAdmin || !c.isInternal())
                .map(c -> {
                    List<TicketAttachment> atts = attachmentRepository.findByCommentIdAndDeletedAtIsNull(c.getId());
                    List<TicketAttachmentResponse> attResponses = atts.stream().map(TicketAttachmentResponse::from).toList();
                    return TicketCommentResponse.from(c, attResponses);
                })
                .toList();
    }

    @Transactional
    public TicketAttachmentResponse addAttachment(Long ticketId, Long commentId, Long userId, String fileName, String originalFileName, String storageKey, String contentType, Long fileSize) {
        SupportTicket ticket = ticketRepository.findByIdWithDetails(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket không tồn tại: " + ticketId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

        TicketComment comment = null;
        if (commentId != null) {
            comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment không tồn tại: " + commentId));
        }

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicket(ticket);
        attachment.setComment(comment);
        attachment.setUploadedBy(user);
        attachment.setFileName(fileName);
        attachment.setOriginalFileName(originalFileName);
        attachment.setStorageKey(storageKey);
        attachment.setContentType(contentType);
        attachment.setFileSize(fileSize);

        attachment = attachmentRepository.save(attachment);
        log.info("Saved attachment {} for ticket {}", attachment.getId(), ticketId);
        return TicketAttachmentResponse.from(attachment);
    }

    private void validateStateTransition(TicketStatus from, TicketStatus to) {
        if (from == TicketStatus.CLOSED && to != TicketStatus.CLOSED) {
            throw new BadRequestException("Ticket đã CLOSED không thể chuyển sang trạng thái khác");
        }
        if (from == TicketStatus.OPEN && to == TicketStatus.CLOSED) {
            throw new BadRequestException("Không thể chuyển trực tiếp từ OPEN sang CLOSED");
        }
    }

    private void recordStatusHistory(SupportTicket ticket, TicketStatus fromStatus, TicketStatus toStatus, User changedBy, String reason) {
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }
}
