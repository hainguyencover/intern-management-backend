package com.example.backend.service;

import com.example.backend.dto.admin.RoleDTOs;
import com.example.backend.dto.request.UpdateRolePermissionsRequest;
import com.example.backend.entity.Permission;
import com.example.backend.entity.Role;
import com.example.backend.repository.PermissionRepository;
import com.example.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleDTOs.RoleResponse> getAllRoles() {
        return roleRepository.findAllWithPermissions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleDTOs.RoleResponse getRoleByCode(String code) {
        Role role = roleRepository.findByCodeWithPermissions(code)
                .orElseThrow(() -> new RuntimeException("Role not found: " + code));
        return mapToResponse(role);
    }

    @Transactional
    public RoleDTOs.RoleResponse updateRolePermissions(Long roleId, UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        Set<Permission> permissions = new HashSet<>();
        for (Long permissionId : request.getPermissionIds()) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));
            permissions.add(permission);
        }

        role.setPermissions(permissions);
        role = roleRepository.save(role);

        log.info("Updated permissions for role: {}", role.getCode());

        return mapToResponse(role);
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    private RoleDTOs.RoleResponse mapToResponse(Role role) {
        List<String> permissionCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());

        return RoleDTOs.RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .permissions((Set<String>) permissionCodes)
                .build();
    }
}
