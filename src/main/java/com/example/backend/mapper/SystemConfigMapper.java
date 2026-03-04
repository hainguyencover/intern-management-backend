package com.example.backend.mapper;

import com.example.backend.dto.response.SystemConfigResponse;
import com.example.backend.entity.SystemConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SystemConfigMapper {
    SystemConfigResponse toResponse(SystemConfig config);
}
