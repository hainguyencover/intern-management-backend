package com.holaho.intern.notification.controller;

import com.holaho.intern.notification.dto.NotificationPreferenceDto;
import com.holaho.intern.notification.dto.NotificationResponse;
import com.holaho.intern.notification.dto.UnreadCountResponse;
import com.holaho.intern.notification.entity.NotificationPreference;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.notification.service.NotificationPreferenceService;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
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
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) NotificationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationResponse> page = notificationService.getNotificationsForRecipient(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count)));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        NotificationResponse response = notificationService.markAsRead(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        notificationService.deleteNotification(id, userDetails.getId());
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
}
