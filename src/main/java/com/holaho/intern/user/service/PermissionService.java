package com.holaho.intern.user.service;

import com.holaho.intern.shared.dto.request.UpdateRolePermissionsRequest;
import com.holaho.intern.shared.dto.response.PermissionResponse;
import com.holaho.intern.shared.dto.response.RoleWithPermissionsResponse;
import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.repository.PermissionRepository;
import com.holaho.intern.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleWithPermissionsResponse getRoleWithPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role khÃ´ng tá»“n táº¡i"));

        RoleWithPermissionsResponse response = new RoleWithPermissionsResponse();
        response.setId(role.getId());
        response.setCode(role.getCode());
        response.setName(role.getName());
        response.setPermissions(role.getPermissions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()));

        return response;
    }

    @Transactional
    public RoleWithPermissionsResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role khÃ´ng tá»“n táº¡i"));

        // Clear existing permissions
        role.setPermissions(new HashSet<>());

        // Add new permissions
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
            role.setPermissions(new HashSet<>(permissions));
        }

        role = roleRepository.save(role);
        return getRoleWithPermissions(role.getId());
    }

    private PermissionResponse mapToResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setCode(permission.getCode());
        response.setName(permission.getName());
        response.setModule(permission.getModule()); // Use module field correctly
        response.setDescription(permission.getDescription());
        return response;
    }
}

