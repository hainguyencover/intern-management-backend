package com.holaho.intern.service;

import com.holaho.intern.shared.dto.request.SupportTicketCreateRequest;
import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.entity.TicketComment;
import com.holaho.intern.entity.User;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketStatus;
import com.holaho.intern.repository.SupportTicketRepository;
import com.holaho.intern.repository.TicketCommentRepository;
import com.holaho.intern.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupportTicket createTicket(SupportTicketCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        SupportTicket ticket = new SupportTicket();
        ticket.setCreatedBy(user);
        ticket.setCategory(request.getCategory());
        ticket.setTitle(request.getTitle());
        ticket.setContent(request.getContent());
        ticket.setStatus(TicketStatus.OPEN);

        ticket = ticketRepository.save(ticket);
        log.info("Created support ticket by user: {}", userId);

        return ticket;
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus status) {
        SupportTicket ticket = ticketRepository.findByIdWithCreator(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        ticket.setStatus(status);

        ticket = ticketRepository.save(ticket);
        log.info("Updated ticket {} status to {}", ticketId, status);

        return ticket;
    }

    @Transactional
    public TicketComment addComment(Long ticketId, Long authorId, String content) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found: " + authorId));

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setContent(content);

        comment = commentRepository.save(comment);
        log.info("Added comment to ticket: {}", ticketId);
        return comment;
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketById(Long id) {
        return ticketRepository.findByIdWithCreator(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> getMyTickets(Long userId, Pageable pageable) {
        return ticketRepository.findByCreatedByIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SupportTicket> searchTickets(
            TicketStatus status, TicketCategory category, Long createdById, String keyword, Pageable pageable) {
        return ticketRepository.search(status, category, createdById, keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<TicketComment> getCommentsByTicketId(Long ticketId) {
        return commentRepository.findByTicketIdWithAuthor(ticketId);
    }
}

