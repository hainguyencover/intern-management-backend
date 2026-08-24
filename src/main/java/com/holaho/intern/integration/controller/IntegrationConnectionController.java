package com.holaho.intern.integration.controller;

import com.holaho.intern.integration.dto.ConnectionTestResponse;
import com.holaho.intern.integration.dto.IntegrationConnectionRequest;
import com.holaho.intern.integration.dto.IntegrationConnectionResponse;
import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.service.IntegrationConnectionService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationConnectionController {

    private final IntegrationConnectionService connectionService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<IntegrationConnectionResponse>>> getAllConnections() {
        List<IntegrationConnectionResponse> responseList = connectionService.getAllConnections()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responseList));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IntegrationConnectionResponse>> createConnection(
            @RequestBody IntegrationConnectionRequest request
    ) {
        IntegrationConnection connection = connectionService.createConnection(
                request.getCode(),
                request.getName(),
                request.getIntegrationType(),
                request.getProvider(),
                request.getBaseUrl(),
                request.getAuthType(),
                request.getCredentials()
        );
        return ResponseEntity.ok(ApiResponse.success("Tạo kết nối tích hợp thành công", mapToResponse(connection)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IntegrationConnectionResponse>> getConnectionById(@PathVariable Long id) {
        IntegrationConnection connection = connectionService.getConnectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + id));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(connection)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<IntegrationConnectionResponse>> updateConnection(
            @PathVariable Long id,
            @RequestBody IntegrationConnectionRequest request
    ) {
        IntegrationConnection updated = connectionService.updateConnection(
                id,
                request.getName(),
                request.getBaseUrl(),
                request.getAuthType(),
                request.getEnabled(),
                request.getCredentials()
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kết nối thành công", mapToResponse(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteConnection(@PathVariable Long id) {
        connectionService.deleteConnection(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa kết nối thành công", "DELETED"));
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConnectionTestResponse>> testConnection(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        boolean success = connectionService.testConnection(id);
        long latency = System.currentTimeMillis() - start;

        ConnectionTestResponse response = ConnectionTestResponse.builder()
                .success(success)
                .message(success ? "Kết nối thành công" : "Kết nối thất bại, vui lòng kiểm tra URL/Credential")
                .latencyMs(latency)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private IntegrationConnectionResponse mapToResponse(IntegrationConnection c) {
        return IntegrationConnectionResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .integrationType(c.getIntegrationType())
                .provider(c.getProvider())
                .baseUrl(c.getBaseUrl())
                .status(c.getStatus())
                .authType(c.getAuthType())
                .enabled(c.getEnabled())
                .lastSyncAt(c.getLastSyncAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
