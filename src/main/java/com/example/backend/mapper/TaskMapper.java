package com.example.backend.mapper;

import com.example.backend.dto.response.TaskResponse;
import com.example.backend.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskMapper {

    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "creatorName", source = "createdBy.fullName")
    @Mapping(target = "createdBy", source = "createdBy.id")
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "assigneeName", source = "assignee.user.fullName")
    @Mapping(target = "progressPercent", source = "progressPercent")
    @Mapping(target = "updateCount", ignore = true)
    TaskResponse toResponse(Task task);
}
