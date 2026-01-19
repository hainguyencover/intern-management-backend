package com.example.backend.repository;

import com.example.backend.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketId(Long ticketId);

    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @Query("SELECT tc FROM TicketComment tc " +
            "JOIN FETCH tc.author " +
            "WHERE tc.ticket.id = :ticketId " +
            "ORDER BY tc.createdAt ASC")
    List<TicketComment> findByTicketIdWithAuthor(@Param("ticketId") Long ticketId);

    @Query("SELECT tc FROM TicketComment tc WHERE tc.author.id = :authorId")
    List<TicketComment> findByAuthorId(@Param("authorId") Long authorId);

    long countByTicketId(Long ticketId);

    long countByAuthorId(Long authorId);
}
