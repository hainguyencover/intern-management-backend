package com.holaho.intern.shared.mapper;

import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.task.entity.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task) {
        if (task == null) return null;
        return TaskResponse.from(task);
    }
}

