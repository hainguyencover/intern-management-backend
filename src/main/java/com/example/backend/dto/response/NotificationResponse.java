package com.example.backend.dto.response;

import com.example.backend.enums.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        boolean read,
        LocalDateTime createdAt
) {}
