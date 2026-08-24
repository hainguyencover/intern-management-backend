package com.holaho.intern.notification.listener;

import com.holaho.intern.entity.UniversityUser;
import com.holaho.intern.intern.event.InternshipStatusChangedEvent;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.UniversityNotificationPreference;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.notification.repository.UniversityNotificationPreferenceRepository;
import com.holaho.intern.repository.UniversityUserRepository;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class UniversityNotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UniversityNotificationPreferenceRepository preferenceRepository;
    private final UniversityUserRepository universityUserRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    @Transactional
    public void handleInternshipStatusChanged(InternshipStatusChangedEvent event) {
        if (event.universityId() == null) {
            log.debug("Skip university notification: universityId is null for internProfileId={}", event.internProfileId());
            return;
        }

        String newStatus = event.newStatus();
        if (!"COMPLETED".equalsIgnoreCase(newStatus) && !"TERMINATED".equalsIgnoreCase(newStatus)) {
            log.debug("Skip university notification: status {} does not trigger notification", newStatus);
            return;
        }

        boolean isCompleted = "COMPLETED".equalsIgnoreCase(newStatus);
        NotificationType type = isCompleted ? NotificationType.INTERNSHIP_COMPLETED : NotificationType.INTERNSHIP_TERMINATED;

        // Check university preferences
        UniversityNotificationPreference pref = preferenceRepository.findByUniversityId(event.universityId())
                .orElseGet(() -> UniversityNotificationPreference.builder()
                        .universityId(event.universityId())
                        .internshipCompletedEnabled(true)
                        .internshipTerminatedEnabled(true)
                        .emailEnabled(true)
                        .inAppEnabled(true)
                        .build());

        if (isCompleted && !pref.isInternshipCompletedEnabled()) {
            log.info("University notification for COMPLETED disabled for universityId={}", event.universityId());
            return;
        }
        if (!isCompleted && !pref.isInternshipTerminatedEnabled()) {
            log.info("University notification for TERMINATED disabled for universityId={}", event.universityId());
            return;
        }

        // Find all staff/admin users for this university
        List<UniversityUser> univUsers = universityUserRepository.findByUniversityId(event.universityId());
        if (univUsers.isEmpty()) {
            log.warn("No university users found for universityId={}", event.universityId());
            return;
        }

        String title = isCompleted
                ? "Sinh viên hoàn thành thực tập: " + event.studentName()
                : "Thông báo chấm dứt thực tập: " + event.studentName();

        String message = isCompleted
                ? String.format("Sinh viên %s (MSSV: %s) đã hoàn thành xuất sắc chương trình thực tập.",
                event.studentName(), event.studentCode() != null ? event.studentCode() : "N/A")
                : String.format("Sinh viên %s (MSSV: %s) đã bị chấm dứt thực tập. Lý do: %s",
                event.studentName(), event.studentCode() != null ? event.studentCode() : "N/A",
                event.reason() != null ? event.reason() : "Quyết định từ đơn vị thực tập.");

        String refType = "INTERNSHIP_STATUS_" + newStatus.toUpperCase();

        for (UniversityUser univUser : univUsers) {
            User user = univUser.getUser();
            if (user == null) continue;

            // Idempotency check: prevent duplicate notifications
            boolean exists = notificationRepository.existsByReferenceTypeAndReferenceIdAndRecipientId(
                    refType, event.internProfileId(), user.getId()
            );
            if (exists) {
                log.info("Notification already exists for referenceType={}, internProfileId={}, recipientId={}",
                        refType, event.internProfileId(), user.getId());
                continue;
            }

            // 1. Save In-App Notification
            if (pref.isInAppEnabled()) {
                try {
                    Notification notification = Notification.builder()
                            .universityId(event.universityId())
                            .recipient(user)
                            .type(type)
                            .title(title)
                            .message(message)
                            .referenceType(refType)
                            .referenceId(event.internProfileId())
                            .status(NotificationStatus.UNREAD)
                            .build();

                    notificationRepository.save(notification);
                    log.info("Saved university notification for user {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to save university notification for user {}", user.getId(), e);
                }
            }

            // 2. Send Email Notification
            if (pref.isEmailEnabled() && user.getEmail() != null) {
                try {
                    String htmlBody = String.format("""
                            <h3>%s</h3>
                            <p><strong>Sinh viên:</strong> %s</p>
                            <p><strong>MSSV:</strong> %s</p>
                            <p><strong>Trạng thái mới:</strong> %s</p>
                            <p><strong>Chi tiết:</strong> %s</p>
                            """, title, event.studentName(), event.studentCode() != null ? event.studentCode() : "N/A", newStatus, message);

                    emailService.sendSimpleEmail(user.getEmail(), title, htmlBody);
                    log.info("Sent email notification to university user {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send email notification to {}", user.getEmail(), e);
                }
            }

            // 3. Realtime WebSocket Push
            try {
                messagingTemplate.convertAndSendToUser(
                        user.getEmail(),
                        "/queue/notifications",
                        title + ": " + message
                );
            } catch (Exception e) {
                log.error("Failed to push websocket to user {}", user.getEmail(), e);
            }
        }
    }
}
