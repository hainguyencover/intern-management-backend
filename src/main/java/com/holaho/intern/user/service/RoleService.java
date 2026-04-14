package com.holaho.intern.user.service;

import com.holaho.intern.user.entity.Permission;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.repository.PermissionRepository;
import com.holaho.intern.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<Role> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        roles.forEach(role -> role.getPermissions().size());
        return roles;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public Set<Permission> getRolePermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        // Initialize lazy collection
        role.getPermissions().size();
        return role.getPermissions();
    }

    @Transactional
    public void updateRolePermissions(Long roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissionRepository.findByCode(code).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);
        roleRepository.save(role);
    }
}

