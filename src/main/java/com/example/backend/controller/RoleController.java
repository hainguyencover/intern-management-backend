package com.example.backend.controller;

import com.example.backend.dto.admin.RoleDTOs;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AuditLogService;
import com.example.backend.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final AuditLogService auditService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleDTOs.RoleResponse>> getRoles() {
        var roles = roleService.getAllRoles().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    public ResponseEntity<List<RoleDTOs.PermissionResponse>> getPermissions() {
        var permissions = roleService.getAllPermissions().stream()
                .map(this::mapPermissionToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<Set<RoleDTOs.PermissionResponse>> getRolePermissions(@PathVariable Long id) {
        var permissions = roleService.getRolePermissions(id).stream()
                .map(this::mapPermissionToResponse)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(permissions);
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<Void> updateRolePermissions(@PathVariable Long id,
            @RequestBody RoleDTOs.UpdateRolePermissionsRequest request,
            org.springframework.security.core.Authentication auth) {

        roleService.updateRolePermissions(id, request.getPermissionCodes());

        // Audit log
        try {
            User actor = userRepository.findByEmail(auth.getName()).orElse(null);
            Long actorId = actor != null ? actor.getId() : null;
            String afterJson = objectMapper.writeValueAsString(request.getPermissionCodes());

            auditService.createAuditLog(
                    actorId,
                    auth.getName(),
                    "UPDATE_ROLE_PERMISSIONS",
                    "ROLE",
                    id,
                    "Updated permissions for role ID: " + id,
                    null,
                    afterJson);
        } catch (Exception e) {
            // Log error but don't fail request
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    private RoleDTOs.RoleResponse mapRoleToResponse(Role role) {
        RoleDTOs.RoleResponse res = new RoleDTOs.RoleResponse();
        res.setId(role.getId());
        res.setCode(role.getCode());
        res.setName(role.getName());
        res.setPermissions(role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));
        return res;
    }

    private RoleDTOs.PermissionResponse mapPermissionToResponse(Permission permission) {
        RoleDTOs.PermissionResponse res = new RoleDTOs.PermissionResponse();
        res.setId(permission.getId());
        res.setCode(permission.getCode());
        res.setName(permission.getName());
        res.setDescription(permission.getDescription());
        return res;
    }
}
