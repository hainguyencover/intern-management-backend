package com.example.backend.service;

import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void create(User user, NotificationType type, String title, String content);

    Page<Notification> getMyNotifications(
            String email, boolean unreadOnly, Pageable pageable
    );

    void markAsRead(Long id, String email);
}
