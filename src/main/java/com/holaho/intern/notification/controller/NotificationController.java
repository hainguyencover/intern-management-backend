package com.holaho.intern.notification.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.NotificationResponse;
import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.notification.dto.NotificationPreferenceDto;
import com.holaho.intern.notification.entity.NotificationPreference;
import com.holaho.intern.notification.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<java.util.List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Notification> page = notificationService.getNotificationsByUserId(userDetails.getId(), pageable);
        Page<NotificationResponse> response = page.map(this::mapToResponse);
        return ResponseEntity.ok(ApiResponse.successPage(response));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Notification> list = notificationService.getUnreadNotifications(userDetails.getId());
        List<NotificationResponse> response = list.stream().map(this::mapToResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        int count = notificationService.getUnreadNotifications(userDetails.getId()).size();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDto.Response>>> getPreferences(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<NotificationPreference> list = preferenceService.getPreferencesByUserId(userDetails.getId());
        List<NotificationPreferenceDto.Response> response = list.stream()
                .map(p -> NotificationPreferenceDto.Response.builder()
                        .id(p.getId())
                        .eventType(p.getEventType())
                        .emailEnabled(p.isEmailEnabled())
                        .websocketEnabled(p.isWebsocketEnabled())
                        .inAppEnabled(p.isInAppEnabled())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> updatePreference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody NotificationPreferenceDto.Request request) {
        preferenceService.updatePreference(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Notification preference updated successfully", null));
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType().name())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

