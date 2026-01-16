package com.example.backend.controller;

import com.example.backend.dto.admin.UserDTOs;
import com.example.backend.service.AdminUserService;
import com.example.backend.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminService;
    private final AuditLogService auditService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserDTOs.UserResponse> createUser(@Valid @RequestBody UserDTOs.CreateUserRequest request) {
        var res = adminService.createUser(request);
        return ResponseEntity.ok(res);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<Page<UserDTOs.UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(keyword, pageable));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
            @RequestBody UserDTOs.UpdateUserStatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Void> assignRoles(@PathVariable Long id, @RequestBody UserDTOs.RoleAssignRequest request) {
        adminService.assignRoles(id, request.getRoleCodes());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-lock")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<Void> toggleLock(@PathVariable Long id) {
        adminService.toggleUserLock(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@PathVariable Long id) {
        String newPass = adminService.resetUserPassword(id);
        return ResponseEntity.ok(java.util.Map.of("password", newPass));
    }
}
