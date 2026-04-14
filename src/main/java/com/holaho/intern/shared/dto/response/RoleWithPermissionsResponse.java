package com.holaho.intern.shared.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RoleWithPermissionsResponse {
    private Long id;
    private String code;
    private String name;
    private List<PermissionResponse> permissions;
}

