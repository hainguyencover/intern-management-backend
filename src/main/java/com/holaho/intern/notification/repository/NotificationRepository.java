package com.holaho.intern.notification.repository;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(
        Long recipientId, NotificationStatus status, Pageable pageable
    );

    long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.recipient.id = :recipientId AND n.status = 'UNREAD'")
    int markAllAsReadForRecipient(@Param("recipientId") Long recipientId, @Param("now") LocalDateTime now);

    // University isolated methods
    Page<Notification> findByUniversityIdOrderByCreatedAtDesc(Long universityId, Pageable pageable);

    Page<Notification> findByUniversityIdAndStatusOrderByCreatedAtDesc(Long universityId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByUniversityIdAndTypeOrderByCreatedAtDesc(Long universityId, com.holaho.intern.shared.enums.NotificationType type, Pageable pageable);

    Page<Notification> findByUniversityIdAndStatusAndTypeOrderByCreatedAtDesc(Long universityId, NotificationStatus status, com.holaho.intern.shared.enums.NotificationType type, Pageable pageable);

    long countByUniversityIdAndStatus(Long universityId, NotificationStatus status);

    Optional<Notification> findByIdAndUniversityId(Long id, Long universityId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.universityId = :universityId AND n.status = 'UNREAD'")
    int markAllAsReadForUniversity(@Param("universityId") Long universityId, @Param("now") LocalDateTime now);

    boolean existsByReferenceTypeAndReferenceIdAndRecipientId(String referenceType, Long referenceId, Long recipientId);
}
