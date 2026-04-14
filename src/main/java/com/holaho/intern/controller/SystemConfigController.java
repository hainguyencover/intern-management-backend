package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.SystemConfigResponse;


import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/system/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<com.holaho.intern.shared.dto.response.SystemConfigResponse>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.success(systemConfigService.getAllConfigs()));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.holaho.intern.shared.dto.response.SystemConfigResponse>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> payload) {
        String value = payload.get("value");
        String description = payload.get("description");
        com.holaho.intern.shared.dto.response.SystemConfigResponse config = systemConfigService.updateConfig(key, value,
                description);
        return ResponseEntity.ok(ApiResponse.success("System configuration updated successfully", config));
    }
}

