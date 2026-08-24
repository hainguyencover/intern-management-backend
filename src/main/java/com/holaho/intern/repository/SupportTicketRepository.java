package com.holaho.intern.repository;

import com.holaho.intern.entity.SupportTicket;
import com.holaho.intern.shared.enums.TicketCategory;
import com.holaho.intern.shared.enums.TicketPriority;
import com.holaho.intern.shared.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long>, JpaSpecificationExecutor<SupportTicket> {

    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo WHERE t.id = :id")
    Optional<SupportTicket> findByIdWithDetails(@Param("id") Long id);

    Optional<SupportTicket> findByTicketCode(String ticketCode);

    Page<SupportTicket> findByCreatedBy_Id(Long userId, Pageable pageable);

    List<SupportTicket> findByStatus(TicketStatus status);

    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

    List<SupportTicket> findByCategory(TicketCategory category);

    Page<SupportTicket> findByCategory(TicketCategory category, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.createdBy.id = :userId")
    List<SupportTicket> findByCreatedById(@Param("userId") Long userId);

    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo " +
            "WHERE t.createdBy.id = :userId " +
            "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByCreatedByIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("SELECT t FROM SupportTicket t LEFT JOIN FETCH t.createdBy LEFT JOIN FETCH t.assignedTo WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:category IS NULL OR t.category = :category) AND " +
            "(:priority IS NULL OR t.priority = :priority) AND " +
            "(:createdById IS NULL OR t.createdBy.id = :createdById) AND " +
            "(:assignedToId IS NULL OR t.assignedTo.id = :assignedToId) AND " +
            "(:overdue IS NULL OR (:overdue = true AND t.firstResponseAt IS NULL AND t.firstResponseDueAt < :now) OR (:overdue = false AND (t.firstResponseAt IS NOT NULL OR t.firstResponseDueAt >= :now))) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.ticketCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SupportTicket> searchEnhanced(
            @Param("status") TicketStatus status,
            @Param("category") TicketCategory category,
            @Param("priority") TicketPriority priority,
            @Param("createdById") Long createdById,
            @Param("assignedToId") Long assignedToId,
            @Param("overdue") Boolean overdue,
            @Param("now") LocalDateTime now,
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByCategory(TicketCategory category);

    long countByCreatedById(Long userId);

    @Query("SELECT t FROM SupportTicket t WHERE t.status IN ('OPEN', 'IN_PROGRESS') AND t.firstResponseAt IS NULL AND t.firstResponseDueAt < :now")
    List<SupportTicket> findOverdueTickets(@Param("now") LocalDateTime now);
}
