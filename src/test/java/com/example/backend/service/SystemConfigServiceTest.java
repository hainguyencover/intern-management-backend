package com.example.backend.service;

import com.example.backend.dto.response.SystemConfigResponse;
import com.example.backend.entity.SystemConfig;
import com.example.backend.mapper.SystemConfigMapper;
import com.example.backend.repository.SystemConfigRepository;
import com.example.backend.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    private SystemConfig config;
    private SystemConfigResponse configResponse;

    @BeforeEach
    void setUp() {
        config = new SystemConfig();
        config.setConfigKey("APP_NAME");
        config.setConfigValue("Antigravity");

        configResponse = new SystemConfigResponse();
        configResponse.setConfigKey("APP_NAME");
        configResponse.setConfigValue("Antigravity");
    }

    @Test
    void getValue_ShouldReturnStoredValue() {
        when(systemConfigRepository.findByConfigKey("APP_NAME")).thenReturn(Optional.of(config));

        String value = systemConfigService.getValue("APP_NAME", "Default");

        assertEquals("Antigravity", value);
    }

    @Test
    void getValue_ShouldReturnDefaultIfNotFound() {
        when(systemConfigRepository.findByConfigKey("MISSING")).thenReturn(Optional.empty());

        String value = systemConfigService.getValue("MISSING", "Default");

        assertEquals("Default", value);
    }

    @Test
    void updateConfig_ShouldSaveAndReturnResponse() {
        when(systemConfigRepository.findByConfigKey("APP_NAME")).thenReturn(Optional.of(config));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenReturn(config);
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(configResponse);

        SystemConfigResponse result = systemConfigService.updateConfig("APP_NAME", "NewValue", "New Desc");

        assertNotNull(result);
        verify(systemConfigRepository).save(any(SystemConfig.class));
    }

    @Test
    void getAllConfigs_ShouldReturnList() {
        when(systemConfigRepository.findAll()).thenReturn(Collections.singletonList(config));
        when(systemConfigMapper.toResponse(any(SystemConfig.class))).thenReturn(configResponse);

        List<SystemConfigResponse> results = systemConfigService.getAllConfigs();

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }
}
