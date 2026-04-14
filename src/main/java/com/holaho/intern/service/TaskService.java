package com.holaho.intern.service;

import com.holaho.intern.shared.dto.TaskUpdateDto;
import com.holaho.intern.shared.dto.request.CreateTaskRequest;
import com.holaho.intern.shared.dto.request.TaskRequest;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.request.UpdateTaskProgressRequest;
import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.entity.TaskUpdate;
import com.holaho.intern.shared.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request, Long creatorId);
    TaskResponse updateTaskProgress(Long taskId, UpdateTaskProgressRequest request, Long internId);
    TaskResponse getTaskById(Long id);
    Page<TaskResponse> getTasksByGroupId(Long groupId, Pageable pageable);
    List<TaskResponse> getOverdueTasks();
    TaskResponse create(TaskRequest request, Long creatorId);
    TaskResponse update(Long id, TaskRequest request, Long userId);
    Page<TaskResponse> getAllTasks(Pageable pageable);
    void updateStatus(Long id, TaskStatus status);
    void addUpdate(Long id, TaskUpdateRequest request, Long internId);
    List<TaskUpdate> getTaskUpdates(Long taskId);
    void deleteTask(Long id);
    TaskResponse getById(Long id);
    TaskUpdateDto internCreateUpdate(Long taskId, Long userId, TaskUpdateRequest req);
    Page<TaskUpdateDto> internListMyTaskUpdates(Long taskId, Long userId, Pageable pageable);
    Page<TaskUpdateDto> mentorListTaskUpdates(Long taskId, Long userId, Pageable pageable);
    Page<TaskResponse> getTasksByGroup(Long groupId, Pageable pageable);
    Page<TaskResponse> getMyAssignedTasks(Long internId, Pageable pageable);
    Page<TaskResponse> getTasksByAssignee(Long assigneeId, Pageable pageable);
    Page<TaskResponse> getAssignedTasks(Long creatorId, Long assigneeId, Long groupId, String status, String keyword, Pageable pageable);
}

