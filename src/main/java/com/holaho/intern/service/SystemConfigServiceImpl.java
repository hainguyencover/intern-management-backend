package com.holaho.intern.service;

import com.holaho.intern.shared.annotation.Auditable;
import com.holaho.intern.shared.dto.response.SystemConfigResponse;
import com.holaho.intern.entity.SystemConfig;
import com.holaho.intern.shared.mapper.SystemConfigMapper;
import com.holaho.intern.repository.SystemConfigRepository;
import com.holaho.intern.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponse> getAllConfigs() {
        return systemConfigRepository.findAll().stream()
                .map(systemConfigMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "systemConfigs", key = "#key")
    public String getValue(String key, String defaultValue) {
        log.debug("Fetching config value for key: {}", key);
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    @CacheEvict(value = "system_configs", key = "#key")
    @Transactional
    @Auditable(action = "UPDATE_CONFIG")
    public SystemConfigResponse updateConfig(String key, String value, String description) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseGet(() -> {
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(key);
                    return newConfig;
                });

        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }

        config = systemConfigRepository.save(config);
        log.info("System configuration updated: {} = {}", key, value);

        return systemConfigMapper.toResponse(config);
    }
}

