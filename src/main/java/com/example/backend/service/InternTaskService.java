package com.example.backend.service;

import com.example.backend.dto.TaskDto;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Task;
import com.example.backend.enums.TaskStatus;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.TaskRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InternTaskService {

    private final TaskRepository taskRepository;
    private final InternProfileRepository internProfileRepository;

    public InternTaskService(TaskRepository taskRepository, InternProfileRepository internProfileRepository) {
        this.taskRepository = taskRepository;
        this.internProfileRepository = internProfileRepository;
    }

    public Page<TaskDto> myTasks(Long internUserId, String status, Pageable pageable) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intern profile not found for user"));

        Page<Task> page;
        if (status == null || status.isBlank()) {
            page = taskRepository.findByAssignee_Id(intern.getId(), pageable);
        } else {
            TaskStatus st;
            try { st = TaskStatus.valueOf(status); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status"); }

            page = taskRepository.findByAssignee_IdAndStatus(intern.getId(), st, pageable);
        }

        return page.map(t -> new TaskDto(
                t.getId(),
                t.getGroup() != null ? t.getGroup().getId() : null,
                t.getTitle(),
                t.getDescription(),
                t.getDueDate(),
                t.getStatus(),
                intern.getId(),
                intern.getUser() != null ? intern.getUser().getFullName() : null
        ));
    }
}
