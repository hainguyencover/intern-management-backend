package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.shared.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Page<Notification> findByUser_Id(Long userId, Pageable pageable);

    long countByUser_IdAndReadFalse(Long userId);

    List<Notification> findByUserId(Long userId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndRead(Long userId, boolean read);

    Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(
            Long userId,
            boolean read,
            Pageable pageable);

    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.id = :userId AND n.read = false " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    @Query("SELECT n FROM Notification n " +
            "WHERE n.user.id = :userId " +
            "AND n.createdAt >= :since " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findRecentByUserId(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    long countByUserIdAndRead(Long userId, boolean read);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids")
    void markAsReadByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :date")
    void deleteOlderThan(@Param("date") LocalDateTime date);

    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(Long userId);
}

