package com.example.backend.controller;

import com.example.backend.entity.Notification;
import com.example.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.backend.dto.response.NotificationResponse;


@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> myNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Pageable pageable
    ) {
        return notificationService
                .getMyNotifications(auth.getName(), unreadOnly, pageable)
                .map(n -> new NotificationResponse(
                        n.getId(),
                        n.getType(),
                        n.getTitle(),
                        n.getContent(),
                        n.isRead(),
                        n.getCreatedAt()
                ));
    }
}
