package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.SystemConfigResponse;
import com.holaho.intern.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    List<SystemConfigResponse> getAllConfigs();
    
    String getValue(String key, String defaultValue);
    
    SystemConfigResponse updateConfig(String key, String value, String description);
}

