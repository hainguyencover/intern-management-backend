package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.TaskUpdateDto;
import com.holaho.intern.entity.TaskUpdate;
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

