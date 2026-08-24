package com.holaho.intern.task.service;

import com.holaho.intern.shared.dto.TaskDto;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternTaskService {

    private final TaskRepository taskRepository;
    private final InternProfileRepository internProfileRepository;

    public InternTaskService(TaskRepository taskRepository, InternProfileRepository internProfileRepository) {
        this.taskRepository = taskRepository;
        this.internProfileRepository = internProfileRepository;
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> myTasks(Long internUserId, String status, Pageable pageable) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId).orElse(null));

        if (intern == null) {
            return Page.empty(pageable);
        }

        Page<Task> page;
        if (status == null || status.isBlank()) {
            page = taskRepository.findByAssignee_Id(intern.getId(), pageable);
        } else {
            TaskStatus st;
            try { st = TaskStatus.valueOf(status); }
            catch (Exception e) { throw new BadRequestException("Invalid status: " + status); }

            page = taskRepository.findByAssignee_IdAndStatus(intern.getId(), st, pageable);
        }

        return page.map(t -> {
            boolean isOverdue = t.getDueDate() != null && java.time.LocalDateTime.now().isAfter(t.getDueDate())
                    && t.getStatus() != TaskStatus.APPROVED && t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED;
            return new TaskDto(
                    t.getId(),
                    t.getGroup() != null ? t.getGroup().getId() : null,
                    t.getTitle(),
                    t.getDescription(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getProgressPercent() != null ? t.getProgressPercent() : 0,
                    t.getStartDate(),
                    t.getDueDate(),
                    t.getWeight(),
                    isOverdue,
                    intern.getId(),
                    intern.getUser() != null ? intern.getUser().getFullName() : null,
                    t.getCreatedAt()
            );
        });
    }
}
