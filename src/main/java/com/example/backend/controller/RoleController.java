package com.example.backend.controller;

import com.example.backend.dto.admin.RoleDTOs;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.PermissionResponse;
import com.example.backend.dto.response.RoleWithPermissionsResponse;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.service.PermissionService;
import com.example.backend.service.RoleService;
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
            @RequestBody com.example.backend.dto.request.UpdateRolePermissionsRequest request) {
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
