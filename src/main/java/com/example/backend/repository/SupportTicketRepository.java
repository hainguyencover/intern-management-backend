package com.example.backend.repository;

import com.example.backend.entity.SupportTicket;
import com.example.backend.enums.TicketCategory;
import com.example.backend.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

        @Query("SELECT t FROM SupportTicket t JOIN FETCH t.createdBy WHERE t.id = :id")
        Optional<SupportTicket> findByIdWithCreator(@Param("id") Long id);

        Page<SupportTicket> findByCreatedBy_Id(Long userId, Pageable pageable);

        List<SupportTicket> findByStatus(TicketStatus status);

        Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);

        List<SupportTicket> findByCategory(TicketCategory category);

        Page<SupportTicket> findByCategory(TicketCategory category, Pageable pageable);

        @Query("SELECT t FROM SupportTicket t WHERE t.createdBy.id = :userId")
        List<SupportTicket> findByCreatedById(@Param("userId") Long userId);

        @Query("SELECT t FROM SupportTicket t JOIN FETCH t.createdBy " +
                        "WHERE t.createdBy.id = :userId " +
                        "ORDER BY t.createdAt DESC")
        Page<SupportTicket> findByCreatedByIdOrderByCreatedAtDesc(
                        @Param("userId") Long userId,
                        Pageable pageable);

        @Query("SELECT t FROM SupportTicket t " +
                        "WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<SupportTicket> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT t FROM SupportTicket t " +
                        "WHERE t.category = :category AND t.status = :status")
        Page<SupportTicket> findByCategoryAndStatus(
                        @Param("category") TicketCategory category,
                        @Param("status") TicketStatus status,
                        Pageable pageable);

        long countByStatus(TicketStatus status);

        long countByCategory(TicketCategory category);

        long countByCreatedById(Long userId);

        @Query("SELECT t FROM SupportTicket t JOIN FETCH t.createdBy WHERE " +
                        "(:status IS NULL OR t.status = :status) AND " +
                        "(:category IS NULL OR t.category = :category) AND " +
                        "(:createdById IS NULL OR t.createdBy.id = :createdById) AND " +
                        "(:keyword IS NULL OR :keyword = '' OR " +
                        "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        Page<SupportTicket> search(
                        @Param("status") TicketStatus status,
                        @Param("category") TicketCategory category,
                        @Param("createdById") Long createdById,
                        @Param("keyword") String keyword,
                        Pageable pageable);
}
