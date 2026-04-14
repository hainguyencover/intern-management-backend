package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}

