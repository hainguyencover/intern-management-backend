package com.holaho.intern.notification.service;

import com.holaho.intern.intern.entity.InternshipContract;
import com.holaho.intern.notification.dto.NotificationResponse;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.enums.NotificationChannel;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationDeliveryService deliveryService;
    private final NotificationPreferenceService preferenceService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(
            User recipient,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId
    ) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .status(NotificationStatus.UNREAD)
                .build();

        Notification saved = notificationRepository.save(notification);

        // Check preferences and create delivery entries
        if (preferenceService.isChannelEnabled(recipient.getId(), type.name(), NotificationChannel.IN_APP)) {
            deliveryService.createDelivery(saved, NotificationChannel.IN_APP);
        }
        if (preferenceService.isChannelEnabled(recipient.getId(), type.name(), NotificationChannel.EMAIL)) {
            deliveryService.createDelivery(saved, NotificationChannel.EMAIL);
        }

        // Trigger real-time WebSocket push if IN_APP is enabled
        sendRealtimeWebSocketNotification(recipient, saved);

        return saved;
    }

    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        return createNotification(user, type, title, message, null, null);
    }

    private void sendRealtimeWebSocketNotification(User recipient, Notification notification) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notification.getId());
            payload.put("type", notification.getType().name());
            payload.put("title", notification.getTitle());
            payload.put("message", notification.getMessage());
            payload.put("referenceType", notification.getReferenceType());
            payload.put("referenceId", notification.getReferenceId());
            payload.put("status", notification.getStatus().name());
            payload.put("createdAt", notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : LocalDateTime.now().toString());

            // Convert to response DTO for STOMP push
            NotificationResponse response = mapToResponse(notification);

            if (recipient.getEmail() != null) {
                messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/notifications", response);
            }
            messagingTemplate.convertAndSend("/topic/notifications/" + recipient.getId(), response);
        } catch (Exception e) {
            log.warn("Could not send real-time WebSocket notification to user {}: {}", recipient.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsForRecipient(
            Long recipientId,
            NotificationStatus status,
            Pageable pageable
    ) {
        Page<Notification> page;
        if (status != null) {
            page = notificationRepository.findByRecipientIdAndStatusOrderByCreatedAtDesc(recipientId, status, pageable);
        } else {
            page = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndStatus(recipientId, NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, Long recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long recipientId) {
        notificationRepository.markAllAsReadForRecipient(recipientId, LocalDateTime.now());
        log.info("Marked all notifications as read for recipient: {}", recipientId);
    }

    @Transactional
    public void deleteNotification(Long id, Long recipientId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        notificationRepository.delete(notification);
        log.info("Deleted notification ID: {} for user {}", id, recipientId);
    }

    @Transactional
    public void sendContractNotification(Long userId, Long contractId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        createNotification(
                user,
                NotificationType.SYSTEM,
                "Hợp đồng thực tập mới",
                "Bạn có hợp đồng thực tập mới. Vui lòng kiểm tra và xác nhận.",
                "CONTRACT",
                contractId
        );
        log.info("Contract notification sent to user {}", userId);
    }

    @Transactional
    public void notifyInternAboutContract(InternshipContract contract) {
        User intern = contract.getApplication().getIntern().getUser();

        createNotification(
                intern,
                NotificationType.SYSTEM,
                "Hợp đồng thực tập đã sẵn sàng",
                "Hợp đồng thực tập của bạn đã được tạo. Vui lòng xem và ký.",
                "CONTRACT",
                contract.getId()
        );
        log.info("Notification sent to intern: {}", intern.getEmail());
    }

    public NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
