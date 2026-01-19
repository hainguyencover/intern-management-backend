package com.example.backend.repository;

import com.example.backend.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:actorId IS NULL OR a.actorId = :actorId) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR a.createdAt <= :toDate)")
    Page<AuditLog> findByFilters(
            @Param("actorId") Long actorId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId);

    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :from AND :to ORDER BY al.createdAt DESC")
    Page<AuditLog> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT al FROM AuditLog al WHERE al.action = :action ORDER BY al.createdAt DESC")
    List<AuditLog> findByAction(@Param("action") String action);

    @Query("SELECT a FROM AuditLog a WHERE a.actorId = :actorId ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(@Param("actorId") Long actorId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActionOrderByCreatedAtDesc(@Param("action") String action, Pageable pageable);

    @Query("SELECT a FROM AuditLog a " +
            "WHERE a.entityType = :entityType AND a.entityId = :entityId " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Query("SELECT a FROM AuditLog a " +
            "WHERE a.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a " +
            "WHERE a.actorId = :actorId " +
            "AND a.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActorIdAndDateRangeOrderByCreatedAtDesc(
            @Param("actorId") Long actorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a " +
            "WHERE a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByStatusOrderByCreatedAtDesc(@Param("status") String status, Pageable pageable);

    long countByAction(String action);

    long countByActorId(Long actorId);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :date")
    void deleteOlderThan(@Param("date") LocalDateTime date);
}
