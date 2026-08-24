package com.holaho.intern.integration.dto;

import com.holaho.intern.integration.enums.ConnectionStatus;
import com.holaho.intern.integration.enums.IntegrationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IntegrationConnectionResponse {
    private Long id;
    private String code;
    private String name;
    private IntegrationType integrationType;
    private String provider;
    private String baseUrl;
    private ConnectionStatus status;
    private String authType;
    private Boolean enabled;
    private LocalDateTime lastSyncAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
