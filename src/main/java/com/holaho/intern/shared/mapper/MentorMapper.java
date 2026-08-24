package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.mentor.entity.Mentor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MentorMapper {

    @Mapping(target = "id", source = "mentor.id")
    @Mapping(target = "userId", source = "mentor.user.id")
    @Mapping(target = "email", source = "mentor.user.email")
    @Mapping(target = "fullName", source = "mentor.fullName")
    @Mapping(target = "phone", source = "mentor.phone")
    @Mapping(target = "title", source = "mentor.title")
    @Mapping(target = "departmentId", source = "mentor.department.id")
    @Mapping(target = "departmentName", source = "mentor.department.name")
    @Mapping(target = "createdAt", source = "mentor.createdAt")
    @Mapping(target = "activeInternsCount", source = "internCount")
    MentorResponse toResponse(Mentor mentor, long internCount);
}
