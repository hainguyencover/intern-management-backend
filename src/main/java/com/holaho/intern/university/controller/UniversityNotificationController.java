package com.holaho.intern.university.controller;

import com.holaho.intern.shared.dto.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.*;
import com.holaho.intern.university.service.UniversityNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/university")
@RequiredArgsConstructor
public class UniversityNotificationController {

    private final UniversityNotificationService notificationService;

    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_READ') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN') or hasRole('UNIVERSITY_VIEWER')")
    public ResponseEntity<ApiResponse<Page<UniversityNotificationResponse>>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UniversityNotificationResponse> page = notificationService.findNotifications(principal, status, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_READ') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN') or hasRole('UNIVERSITY_VIEWER')")
    public ResponseEntity<ApiResponse<UniversityNotificationUnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UniversityNotificationUnreadCountResponse response = notificationService.getUnreadCount(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/notifications/{id}")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_READ') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN') or hasRole('UNIVERSITY_VIEWER')")
    public ResponseEntity<ApiResponse<UniversityNotificationResponse>> getNotificationDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id
    ) {
        UniversityNotificationResponse response = notificationService.getNotificationDetail(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/notifications/{id}/read")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_MANAGE') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN')")
    public ResponseEntity<ApiResponse<UniversityNotificationResponse>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id
    ) {
        UniversityNotificationResponse response = notificationService.markAsRead(principal, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/notifications/read-all")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_MANAGE') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        int updatedCount = notificationService.markAllAsRead(principal);
        return ResponseEntity.ok(ApiResponse.success(updatedCount));
    }

    @GetMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_READ') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN')")
    public ResponseEntity<ApiResponse<UniversityNotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UniversityNotificationPreferenceResponse response = notificationService.getPreference(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/notification-preferences")
    @PreAuthorize("hasAuthority('UNIVERSITY_NOTIFICATION_MANAGE') or hasRole('ADMIN') or hasRole('UNIVERSITY_ADMIN')")
    public ResponseEntity<ApiResponse<UniversityNotificationPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody UniversityNotificationPreferenceUpdateRequest request
    ) {
        UniversityNotificationPreferenceResponse response = notificationService.updatePreference(principal, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
