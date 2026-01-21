package com.example.backend.repository;

import com.example.backend.entity.SupportTicket;
import com.example.backend.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    @EntityGraph(attributePaths = {"createdBy"})
    Page<SupportTicket> findByCreatedBy_Id(Long userId, Pageable pageable);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Optional<SupportTicket> findWithCreatedById(Long id);


    @Override
    @EntityGraph(attributePaths = {"createdBy"})
    Page<SupportTicket> findAll(Pageable pageable);
}
