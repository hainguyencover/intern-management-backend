package com.example.backend.controller;

import com.example.backend.dto.admin.RoleDTOs;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final com.example.backend.service.PermissionService permissionService; // Injected

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasRole('ADMIN')")
    public ResponseEntity<List<RoleDTOs.RoleResponse>> getRoles() {
        var roles = roleService.getAllRoles().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    // ========== Permissions Management (Updated for RolePermissionsPage)
    // ==========

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.example.backend.dto.response.PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.example.backend.dto.response.RoleWithPermissionsResponse> getRolePermissions(
            @PathVariable Long id) {
        return ResponseEntity.ok(permissionService.getRoleWithPermissions(id));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.example.backend.dto.response.RoleWithPermissionsResponse> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody com.example.backend.dto.request.UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(permissionService.updateRolePermissions(id, request));
    }

    private RoleDTOs.RoleResponse mapRoleToResponse(Role role) {
        RoleDTOs.RoleResponse res = new RoleDTOs.RoleResponse();
        res.setId(role.getId());
        res.setCode(role.getCode());
        res.setName(role.getName());
        res.setPermissions(role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));
        return res;
    }
}
