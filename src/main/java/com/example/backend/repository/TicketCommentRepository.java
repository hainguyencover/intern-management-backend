package com.example.backend.repository;

import com.example.backend.entity.TicketComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<TicketComment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);
}
