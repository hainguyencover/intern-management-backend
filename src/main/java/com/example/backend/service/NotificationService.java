package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.enums.*;
import com.example.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(Long userId, NotificationType type, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRead(false);

        notification = notificationRepository.save(notification);
        log.info("Created notification for user: {}", userId);

        return notification;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByUserId(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all notifications as read for user: {}", userId);
    }

    @Transactional
    public void sendContractNotification(Long userId, Long contractId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("Hợp đồng thực tập mới");
        notification.setContent("Bạn có hợp đồng thực tập mới. Vui lòng kiểm tra và xác nhận.");
        notification.setRead(false);

        notificationRepository.save(notification);
        log.info("Contract notification sent to user {}", userId);
    }

    @Transactional
    public void notifyInternAboutContract(InternshipContract contract) {
        User intern = contract.getApplication().getIntern().getUser();

        Notification notification = new Notification();
        notification.setUser(intern);
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("Hợp đồng thực tập đã sẵn sàng");
        notification.setContent("Hợp đồng thực tập của bạn đã được tạo. Vui lòng xem và ký.");
        notification.setRead(false);

        notificationRepository.save(notification);
        log.info("Notification sent to intern: {}", intern.getEmail());
    }
}
