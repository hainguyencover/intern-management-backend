package com.holaho.intern.user.controller;

import com.holaho.intern.shared.dto.request.UpdateRolePermissionsRequest;


import com.holaho.intern.shared.dto.admin.RoleDTOs;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.PermissionResponse;
import com.holaho.intern.shared.dto.response.RoleWithPermissionsResponse;
import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.service.PermissionService;
import com.holaho.intern.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RoleDTOs.RoleResponse>>> getRoles() {
        var roles = roleService.getAllRoles().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getAllPermissions()));
    }

    @GetMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleWithPermissionsResponse>> getRolePermissions(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.getRoleWithPermissions(id)));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoleWithPermissionsResponse>> updateRolePermissions(
            @PathVariable Long id,
            @RequestBody com.holaho.intern.shared.dto.request.UpdateRolePermissionsRequest request) {
        RoleWithPermissionsResponse response = permissionService.updateRolePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role permissions updated successfully", response));
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

