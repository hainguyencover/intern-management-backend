package com.example.backend.mapper;

import com.example.backend.dto.response.MentorResponse;
import com.example.backend.entity.Mentor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MentorMapper {

    @Mapping(target = "id", source = "mentor.id")
    @Mapping(target = "userId", source = "mentor.user.id")
    @Mapping(target = "email", source = "mentor.user.email")
    @Mapping(target = "fullName", source = "mentor.user.fullName")
    @Mapping(target = "phone", source = "mentor.user.phone")
    @Mapping(target = "title", source = "mentor.title")
    @Mapping(target = "departmentId", source = "mentor.department.id")
    @Mapping(target = "departmentName", source = "mentor.department.name")
    @Mapping(target = "createdAt", source = "mentor.createdAt")
    @Mapping(target = "internCount", source = "internCount")
    MentorResponse toResponse(Mentor mentor, long internCount);
}
