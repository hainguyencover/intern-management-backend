package com.holaho.intern.service;

import com.holaho.intern.entity.InternshipContract;
import com.holaho.intern.entity.Notification;
import com.holaho.intern.repository.NotificationRepository;
import com.holaho.intern.entity.User;
import com.holaho.intern.repository.UserRepository;
import com.holaho.intern.shared.enums.NotificationType;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

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

        // Send real-time via WebSocket
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                notification);

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
        notification.setTitle("HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thÃ¡Â»Â±c tÃ¡ÂºÂ­p mÃ¡Â»â€ºi");
        notification.setContent("BÃ¡ÂºÂ¡n cÃƒÂ³ hÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thÃ¡Â»Â±c tÃ¡ÂºÂ­p mÃ¡Â»â€ºi. Vui lÃƒÂ²ng kiÃ¡Â»Æ’m tra vÃƒÂ  xÃƒÂ¡c nhÃ¡ÂºÂ­n.");
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
        notification.setTitle("HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thÃ¡Â»Â±c tÃ¡ÂºÂ­p Ã„â€˜ÃƒÂ£ sÃ¡ÂºÂµn sÃƒÂ ng");
        notification.setContent("HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thÃ¡Â»Â±c tÃ¡ÂºÂ­p cÃ¡Â»Â§a bÃ¡ÂºÂ¡n Ã„â€˜ÃƒÂ£ Ã„â€˜Ã†Â°Ã¡Â»Â£c tÃ¡ÂºÂ¡o. Vui lÃƒÂ²ng xem vÃƒÂ  kÃƒÂ½.");
        notification.setRead(false);

        notificationRepository.save(notification);
        log.info("Notification sent to intern: {}", intern.getEmail());
    }
}

