package com.example.backend.service.impl;

import com.example.backend.dto.request.SupportTicketCreateRequest;
import com.example.backend.dto.request.SupportTicketStatusUpdateRequest;
import com.example.backend.dto.request.TicketCommentCreateRequest;
import com.example.backend.dto.response.*;
import com.example.backend.entity.SupportTicket;
import com.example.backend.entity.TicketComment;
import com.example.backend.entity.User;
import com.example.backend.enums.TicketStatus;
import com.example.backend.repository.SupportTicketRepository;
import com.example.backend.repository.TicketCommentRepository;
import com.example.backend.repository.UserRepository; // nếu bạn có
import com.example.backend.service.SupportTicketService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final UserRepository userRepo;

    public SupportTicketServiceImpl(SupportTicketRepository ticketRepo,
                                    TicketCommentRepository commentRepo,
                                    UserRepository userRepo) {
        this.ticketRepo = ticketRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
    }

    // ===== helper: get current user =====
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // thường là email trong JWT
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private SupportTicketResponse toTicketRes(SupportTicket t) {
        return new SupportTicketResponse(
                t.getId(),
                t.getTitle(),
                t.getContent(),
                t.getCategory(),
                t.getStatus(),
                t.getCreatedBy().getId(),
                t.getCreatedBy().getEmail(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    private TicketCommentResponse toCommentRes(TicketComment c) {
        return new TicketCommentResponse(
                c.getId(),
                c.getAuthor().getId(),
                c.getAuthor().getEmail(),
                c.getContent(),
                c.getCreatedAt()
        );
    }

    // ================= INTERN =================
    @Override
    @Transactional
    public SupportTicketResponse createTicket(SupportTicketCreateRequest req) {
        User me = currentUser();

        SupportTicket t = new SupportTicket();
        t.setTitle(req.getTitle());
        t.setContent(req.getContent());
        t.setCategory(req.getCategory());
        t.setStatus(TicketStatus.OPEN);
        t.setCreatedBy(me);

        SupportTicket saved = ticketRepo.save(t);
        return toTicketRes(saved);
    }

    @Override
    public Page<SupportTicketResponse> getMyTickets(int page, int size) {
        User me = currentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ticketRepo.findByCreatedBy_Id(me.getId(), pageable).map(this::toTicketRes);
    }

    @Override
    public SupportTicketDetailResponse getMyTicketDetail(Long ticketId) {
        User me = currentUser();
        SupportTicket t = ticketRepo.findWithCreatedById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // check quyền: intern chỉ xem ticket của mình
        if (!t.getCreatedBy().getId().equals(me.getId())) {
            throw new RuntimeException("Forbidden");
        }

        List<TicketCommentResponse> comments =
                commentRepo.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                        .stream().map(this::toCommentRes).toList();

        return new SupportTicketDetailResponse(toTicketRes(t), comments);
    }

    @Override
    @Transactional
    public void addMyComment(Long ticketId, TicketCommentCreateRequest req) {
        User me = currentUser();
        SupportTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!t.getCreatedBy().getId().equals(me.getId())) {
            throw new RuntimeException("Forbidden");
        }

        TicketComment c = new TicketComment();
        c.setTicket(t);
        c.setAuthor(me);
        c.setContent(req.getContent());

        commentRepo.save(c);
    }

    // ================= HR =================
    @Override
    public Page<SupportTicketResponse> getAllTickets(Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 200) ? 20 : size;

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ticketRepo.findAll(pageable).map(this::toTicketRes);
    }

    @Override
    public SupportTicketDetailResponse getTicketDetailForHr(Long ticketId) {
        SupportTicket t = ticketRepo.findWithCreatedById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        List<TicketCommentResponse> comments =
                commentRepo.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                        .stream().map(this::toCommentRes).toList();

        return new SupportTicketDetailResponse(toTicketRes(t), comments);
    }

    @Override
    @Transactional
    public void addHrComment(Long ticketId, TicketCommentCreateRequest req) {
        User hr = currentUser();
        SupportTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        TicketComment c = new TicketComment();
        c.setTicket(t);
        c.setAuthor(hr);
        c.setContent(req.getContent());

        commentRepo.save(c);

        // Optional: HR comment thì đổi trạng thái sang IN_PROGRESS nếu đang OPEN
        if (t.getStatus() == TicketStatus.OPEN) {
            t.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepo.save(t);
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long ticketId, SupportTicketStatusUpdateRequest req) {
        SupportTicket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        t.setStatus(req.getStatus());
        ticketRepo.save(t);
    }
}
