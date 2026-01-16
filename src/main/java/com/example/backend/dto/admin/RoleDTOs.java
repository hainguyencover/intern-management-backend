package com.example.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public class RoleDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleResponse {
        private Long id;
        private String code;
        private String name;
        private Set<String> permissions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionResponse {
        private Long id;
        private String code;
        private String name;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRolePermissionsRequest {
        private Set<String> permissionCodes;
    }
}
