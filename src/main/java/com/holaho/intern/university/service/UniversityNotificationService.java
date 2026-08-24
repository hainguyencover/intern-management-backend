package com.holaho.intern.university.service;

import com.holaho.intern.exception.ResourceNotFoundException;
import com.holaho.intern.exception.UnauthorizedException;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.UniversityNotificationPreference;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.repository.NotificationRepository;
import com.holaho.intern.notification.repository.UniversityNotificationPreferenceRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversityNotificationService {

    private final NotificationRepository notificationRepository;
    private final UniversityNotificationPreferenceRepository preferenceRepository;

    private Long resolveUniversityId(CustomUserDetails principal) {
        if (principal == null || principal.getUniversityId() == null) {
            throw new UnauthorizedException("Tài khoản không thuộc trường đại học nào");
        }
        return principal.getUniversityId();
    }

    @Transactional(readOnly = true)
    public Page<UniversityNotificationResponse> findNotifications(
            CustomUserDetails principal,
            String statusStr,
            String typeStr,
            Pageable pageable
    ) {
        Long universityId = resolveUniversityId(principal);

        NotificationStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = NotificationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        NotificationType type = null;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                type = NotificationType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<Notification> page;
        if (status != null && type != null) {
            page = notificationRepository.findByUniversityIdAndStatusAndTypeOrderByCreatedAtDesc(universityId, status, type, pageable);
        } else if (status != null) {
            page = notificationRepository.findByUniversityIdAndStatusOrderByCreatedAtDesc(universityId, status, pageable);
        } else if (type != null) {
            page = notificationRepository.findByUniversityIdAndTypeOrderByCreatedAtDesc(universityId, type, pageable);
        } else {
            page = notificationRepository.findByUniversityIdOrderByCreatedAtDesc(universityId, pageable);
        }

        return page.map(UniversityNotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UniversityNotificationUnreadCountResponse getUnreadCount(CustomUserDetails principal) {
        Long universityId = resolveUniversityId(principal);
        long count = notificationRepository.countByUniversityIdAndStatus(universityId, NotificationStatus.UNREAD);
        return new UniversityNotificationUnreadCountResponse(count);
    }

    @Transactional(readOnly = true)
    public UniversityNotificationResponse getNotificationDetail(CustomUserDetails principal, Long id) {
        Long universityId = resolveUniversityId(principal);
        Notification notification = notificationRepository.findByIdAndUniversityId(id, universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc trường của bạn"));
        return UniversityNotificationResponse.fromEntity(notification);
    }

    @Transactional
    public UniversityNotificationResponse markAsRead(CustomUserDetails principal, Long id) {
        Long universityId = resolveUniversityId(principal);
        Notification notification = notificationRepository.findByIdAndUniversityId(id, universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo không tồn tại hoặc không thuộc trường của bạn"));

        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return UniversityNotificationResponse.fromEntity(notification);
    }

    @Transactional
    public int markAllAsRead(CustomUserDetails principal) {
        Long universityId = resolveUniversityId(principal);
        return notificationRepository.markAllAsReadForUniversity(universityId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public UniversityNotificationPreferenceResponse getPreference(CustomUserDetails principal) {
        Long universityId = resolveUniversityId(principal);
        UniversityNotificationPreference pref = preferenceRepository.findByUniversityId(universityId)
                .orElseGet(() -> UniversityNotificationPreference.builder()
                        .universityId(universityId)
                        .internshipCompletedEnabled(true)
                        .internshipTerminatedEnabled(true)
                        .emailEnabled(true)
                        .inAppEnabled(true)
                        .build());
        return UniversityNotificationPreferenceResponse.fromEntity(pref);
    }

    @Transactional
    public UniversityNotificationPreferenceResponse updatePreference(
            CustomUserDetails principal,
            UniversityNotificationPreferenceUpdateRequest request
    ) {
        Long universityId = resolveUniversityId(principal);
        UniversityNotificationPreference pref = preferenceRepository.findByUniversityId(universityId)
                .orElseGet(() -> UniversityNotificationPreference.builder()
                        .universityId(universityId)
                        .build());

        if (request.getInternshipCompletedEnabled() != null) {
            pref.setInternshipCompletedEnabled(request.getInternshipCompletedEnabled());
        }
        if (request.getInternshipTerminatedEnabled() != null) {
            pref.setInternshipTerminatedEnabled(request.getInternshipTerminatedEnabled());
        }
        if (request.getEmailEnabled() != null) {
            pref.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getInAppEnabled() != null) {
            pref.setInAppEnabled(request.getInAppEnabled());
        }

        UniversityNotificationPreference saved = preferenceRepository.save(pref);
        return UniversityNotificationPreferenceResponse.fromEntity(saved);
    }
}
