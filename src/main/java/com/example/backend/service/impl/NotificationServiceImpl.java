package com.example.backend.service.impl;

import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.NotificationService;
import com.example.backend.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    @Override
    public void create(User user, NotificationType type, String title, String content) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRead(false);
        notificationRepo.save(n);
    }

    @Override
    public Page<Notification> getMyNotifications(String email, boolean unreadOnly, Pageable pageable) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return unreadOnly
                ? notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId(), pageable)
                : notificationRepo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
    }

    @Override
    public void markAsRead(Long id, String email) {
        Notification n = notificationRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (n.getUser() == null || !n.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("Not your notification");
        }

        n.setRead(true);
        notificationRepo.save(n);
    }
}
