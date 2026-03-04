package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.service.SystemConfigService;
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
    public ResponseEntity<ApiResponse<List<com.example.backend.dto.response.SystemConfigResponse>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.success(systemConfigService.getAllConfigs()));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.example.backend.dto.response.SystemConfigResponse>> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> payload) {
        String value = payload.get("value");
        String description = payload.get("description");
        com.example.backend.dto.response.SystemConfigResponse config = systemConfigService.updateConfig(key, value,
                description);
        return ResponseEntity.ok(ApiResponse.success("System configuration updated successfully", config));
    }
}
