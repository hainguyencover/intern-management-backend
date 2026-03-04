package com.example.backend.service;

import com.example.backend.dto.response.SystemConfigResponse;
import com.example.backend.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    List<SystemConfigResponse> getAllConfigs();
    
    String getValue(String key, String defaultValue);
    
    SystemConfigResponse updateConfig(String key, String value, String description);
}
