package com.holaho.intern.user.service;

import com.holaho.intern.shared.dto.request.UpdateRolePermissionsRequest;
import com.holaho.intern.shared.dto.request.UpdateUserPermissionsRequest;
import com.holaho.intern.shared.dto.response.PermissionResponse;
import com.holaho.intern.shared.dto.response.RoleWithPermissionsResponse;
import com.holaho.intern.shared.dto.response.UserPermissionsResponse;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.entity.UserPermission;
import com.holaho.intern.user.repository.PermissionRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserPermissionRepository;
import com.holaho.intern.user.repository.UserRepository;
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
    private final UserPermissionRepository userPermissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscCodeAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleWithPermissionsResponse getRoleWithPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role không tồn tại"));

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
                .orElseThrow(() -> new NotFoundException("Role không tồn tại"));

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

    @Transactional(readOnly = true)
    public UserPermissionsResponse getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

        List<UserPermission> overrides = userPermissionRepository.findByUserId(userId);
        List<UserPermissionsResponse.PermissionOverrideDetail> details = overrides.stream()
                .map(o -> UserPermissionsResponse.PermissionOverrideDetail.builder()
                        .permissionId(o.getPermission().getId())
                        .permissionCode(o.getPermission().getCode())
                        .permissionName(o.getPermission().getName())
                        .mode(o.getMode())
                        .build())
                .collect(Collectors.toList());

        return UserPermissionsResponse.builder()
                .userId(user.getId())
                .overrides(details)
                .build();
    }

    @Transactional
    public UserPermissionsResponse updateUserPermissions(Long userId, UpdateUserPermissionsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

        // 1. Remove all old custom permissions for this user
        List<UserPermission> existing = userPermissionRepository.findByUserId(userId);
        userPermissionRepository.deleteAll(existing);
        userPermissionRepository.flush();

        // 2. Insert new permission overrides
        if (request.getOverrides() != null) {
            for (var override : request.getOverrides()) {
                Permission permission = permissionRepository.findById(override.getPermissionId())
                        .orElseThrow(() -> new NotFoundException("Permission không tồn tại: " + override.getPermissionId()));

                UserPermission userPerm = new UserPermission();
                userPerm.setUser(user);
                userPerm.setPermission(permission);
                userPerm.setMode(override.getMode());
                userPermissionRepository.save(userPerm);
            }
        }

        return getUserPermissions(userId);
    }

    private PermissionResponse mapToResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setCode(permission.getCode());
        response.setName(permission.getName());
        response.setModule(permission.getModule());
        response.setDescription(permission.getDescription());
        return response;
    }
}
