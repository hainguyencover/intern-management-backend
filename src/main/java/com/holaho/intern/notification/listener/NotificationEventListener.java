package com.holaho.intern.notification.listener;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.NotificationPreference;
import com.holaho.intern.notification.repository.NotificationPreferenceRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.shared.events.DomainEvents.*;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private boolean isChannelEnabled(Long userId, String eventType, String channel) {
        Optional<NotificationPreference> prefOpt = preferenceRepository.findByUser_IdAndEventType(userId, eventType);
        if (prefOpt.isEmpty()) {
            return true; // default enabled
        }
        NotificationPreference pref = prefOpt.get();
        if ("EMAIL".equalsIgnoreCase(channel)) return pref.isEmailEnabled();
        if ("WEBSOCKET".equalsIgnoreCase(channel)) return pref.isWebsocketEnabled();
        if ("IN_APP".equalsIgnoreCase(channel)) return pref.isInAppEnabled();
        return true;
    }

    private void dispatch(Long userId, String eventType, String title, String content, NotificationType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        // 1. In-App Notification
        if (isChannelEnabled(userId, eventType, "IN_APP")) {
            Notification n = new Notification();
            n.setUser(user);
            n.setTitle(title);
            n.setContent(content);
            n.setType(type);
            n.setRead(false);
            notificationService.save(n);
        }

        // 2. Email
        if (isChannelEnabled(userId, eventType, "EMAIL")) {
            emailService.sendSimpleEmail(user.getEmail(), title, "<p>" + content + "</p>");
        }

        // 3. WebSocket Realtime Push
        if (isChannelEnabled(userId, eventType, "WEBSOCKET")) {
            try {
                messagingTemplate.convertAndSendToUser(
                        user.getEmail(),
                        "/queue/notifications",
                        title + ": " + content
                );
                log.info("WebSocket notification pushed to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to push websocket notification", e);
            }
        }
    }

    @EventListener
    public void handleApplicationAccepted(ApplicationAcceptedEvent event) {
        log.info("Handling ApplicationAcceptedEvent for application ID: {}", event.getApplicationId());
        dispatch(
                event.getUserId(),
                "ApplicationAcceptedEvent",
                "Chúc mừng! Đơn ứng tuyển được chấp nhận",
                String.format("Xin chào %s, đơn ứng tuyển của bạn đã được duyệt thành công.", event.getCandidateName()),
                NotificationType.APPLICATION
        );
    }

    @EventListener
    public void handleTaskAssigned(TaskAssignedEvent event) {
        log.info("Handling TaskAssignedEvent for task ID: {}", event.getTaskId());
        dispatch(
                event.getAssigneeUserId(),
                "TaskAssignedEvent",
                "Công việc mới được giao: " + event.getTaskTitle(),
                String.format("Bạn đã được giao công việc '%s' bởi %s. Hạn chót: %s.", event.getTaskTitle(), event.getCreatorName(), event.getDueDate()),
                NotificationType.TASK
        );
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        log.info("Handling TaskCompletedEvent for task ID: {}", event.getTaskId());
        dispatch(
                event.getCreatorUserId(),
                "TaskCompletedEvent",
                "Nhiệm vụ đã được hoàn thành",
                String.format("Thực tập sinh %s đã hoàn thành nhiệm vụ '%s'.", event.getInternName(), event.getTaskTitle()),
                NotificationType.TASK
        );
    }

    @EventListener
    public void handleWeeklyReportSubmitted(WeeklyReportSubmittedEvent event) {
        log.info("Handling WeeklyReportSubmittedEvent for report ID: {}", event.getReportId());
        dispatch(
                event.getMentorUserId(),
                "WeeklyReportSubmittedEvent",
                "Báo cáo tuần mới từ " + event.getInternName(),
                String.format("Thực tập sinh %s đã nộp báo cáo tuần (%s). Vui lòng đánh giá.", event.getInternName(), event.getWeekRange()),
                NotificationType.SYSTEM
        );
    }

    @EventListener
    public void handleWeeklyReportReviewed(WeeklyReportReviewedEvent event) {
        log.info("Handling WeeklyReportReviewedEvent for report ID: {}", event.getReportId());
        dispatch(
                event.getInternUserId(),
                "WeeklyReportReviewedEvent",
                "Báo cáo tuần đã được nhận xét",
                String.format("Mentor %s đã nhận xét báo cáo tuần của bạn. Đánh giá: %s.", event.getMentorName(), event.getAssessment()),
                NotificationType.SYSTEM
        );
    }

    @EventListener
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        log.info("Handling InterviewScheduledEvent for interview ID: {}", event.getInterviewId());
        dispatch(
                event.getCandidateUserId(),
                "InterviewScheduledEvent",
                "Lịch hẹn phỏng vấn mới",
                String.format("Lịch phỏng vấn của bạn đã được lên lúc %s.", event.getDateTime()),
                NotificationType.APPLICATION
        );
    }

    @EventListener
    public void handleInternAssigned(InternAssignedEvent event) {
        log.info("Handling InternAssignedEvent for intern ID: {}", event.getInternId());
        dispatch(
                event.getMentorUserId(),
                "InternAssignedEvent",
                "Thực tập sinh mới được gán",
                String.format("Thực tập sinh %s đã được phân bổ cho bạn hướng dẫn.", event.getInternName()),
                NotificationType.SYSTEM
        );
    }

    @EventListener
    public void handlePasswordChanged(PasswordChangedEvent event) {
        log.info("Handling PasswordChangedEvent for user ID: {}", event.getUserId());
        dispatch(
                event.getUserId(),
                "PasswordChangedEvent",
                "Mật khẩu tài khoản đã thay đổi",
                "Mật khẩu tài khoản của bạn đã được thay đổi thành công. Vui lòng liên hệ quản trị viên nếu bạn không thực hiện việc này.",
                NotificationType.SYSTEM
        );
    }
}
