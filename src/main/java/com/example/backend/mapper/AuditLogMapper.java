package com.example.backend.mapper;

import com.example.backend.dto.response.AuditLogResponse;
import com.example.backend.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditLogMapper {
    AuditLogResponse toResponse(AuditLog auditLog);
}
