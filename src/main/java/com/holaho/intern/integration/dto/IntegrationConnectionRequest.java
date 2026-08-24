package com.holaho.intern.integration.dto;

import com.holaho.intern.integration.enums.IntegrationType;
import lombok.Data;

import java.util.Map;

@Data
public class IntegrationConnectionRequest {
    private String code;
    private String name;
    private IntegrationType integrationType;
    private String provider;
    private String baseUrl;
    private String authType;
    private Boolean enabled;
    private Map<String, String> credentials;
}
