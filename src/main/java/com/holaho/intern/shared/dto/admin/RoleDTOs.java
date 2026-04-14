package com.holaho.intern.shared.dto.admin;

import com.holaho.intern.shared.dto.request.UpdateRolePermissionsRequest;
import com.holaho.intern.shared.dto.response.PermissionResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

public class RoleDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleResponse {
        private Long id;
        private String code;
        private String name;
        private Set<String> permissions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionResponse {
        private Long id;
        private String code;
        private String name;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRolePermissionsRequest {
        private Set<String> permissionCodes;
    }
}

