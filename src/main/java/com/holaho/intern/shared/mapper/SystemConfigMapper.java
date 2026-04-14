package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.response.SystemConfigResponse;
import com.holaho.intern.entity.SystemConfig;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SystemConfigMapper {
    SystemConfigResponse toResponse(SystemConfig config);
}

