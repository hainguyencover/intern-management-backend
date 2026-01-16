package com.example.backend.dto.response;

import lombok.Data;

@Data
public class PermissionResponse {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
}
