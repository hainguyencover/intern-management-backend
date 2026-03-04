package com.example.backend.mapper;

import com.example.backend.dto.TaskUpdateDto;
import com.example.backend.entity.TaskUpdate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TaskUpdateMapper {

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "internId", source = "intern.id")
    @Mapping(target = "internName", source = "intern.user.fullName")
    TaskUpdateDto toDto(TaskUpdate update);
}
