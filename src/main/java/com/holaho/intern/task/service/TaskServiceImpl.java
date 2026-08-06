package com.holaho.intern.task.service;

import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.entity.TaskUpdate;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.task.repository.TaskUpdateRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.exception.ForbiddenException;


import com.holaho.intern.shared.dto.TaskUpdateDto;
import com.holaho.intern.shared.dto.request.CreateTaskRequest;
import com.holaho.intern.shared.dto.request.TaskRequest;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.request.UpdateTaskProgressRequest;
import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.shared.elasticsearch.service.SearchService;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.mapper.TaskMapper;
import com.holaho.intern.shared.mapper.TaskUpdateMapper;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.task.service.TaskService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskUpdateRepository taskUpdateRepository;
    private final ProgramGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    private final TaskMapper taskMapper;
    private final TaskUpdateMapper taskUpdateMapper;
    private final SearchService searchService;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long creatorId) {
        ProgramGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group", request.getGroupId()));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("User", creatorId));

        Task task = new Task();
        task.setGroup(group);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setCreatedBy(creator);

        if (request.getAssigneeId() != null) {
            InternProfile assignee = internProfileRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new NotFoundException("Assignee", request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        task = taskRepository.save(task);
        searchService.indexTask(task);
        log.info("Created task: {}", task.getTitle());

        if (task.getAssignee() != null && task.getAssignee().getUser().getEmail() != null) {
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskAssignedEvent(
                    this,
                    task.getId(),
                    task.getAssignee().getUser().getId(),
                    task.getAssignee().getUser().getEmail(),
                    task.getAssignee().getUser().getFullName(),
                    task.getTitle(),
                    creator.getFullName(),
                    task.getDueDate() != null ? task.getDueDate().toString() : "Không có"
            ));
        }

        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskProgress(Long taskId, UpdateTaskProgressRequest request, Long internId) {
        Task task = taskRepository.findByIdWithGroup(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("InternProfile", internId));

        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setIntern(intern);
        update.setProgressPercent(request.getProgressPercent());
        update.setContent(request.getContent());

        taskUpdateRepository.save(update);

        task.setProgressPercent(request.getProgressPercent());
        boolean justCompleted = false;
        if (request.getProgressPercent() >= 100) {
            if (task.getStatus() != TaskStatus.DONE) {
                justCompleted = true;
            }
            task.setStatus(TaskStatus.DONE);
        } else if (request.getProgressPercent() > 0) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        task = taskRepository.save(task);

        if (justCompleted && task.getCreatedBy() != null) {
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskCompletedEvent(
                    this,
                    task.getId(),
                    task.getCreatedBy().getId(),
                    intern.getUser().getFullName(),
                    task.getTitle()
            ));
        }
        searchService.indexTask(task);
        log.info("Updated task {} progress to {}%", taskId, request.getProgressPercent());

        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findByIdWithGroup(id)
                .orElseThrow(() -> new NotFoundException("Task", id));
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByGroupId(Long groupId, Pageable pageable) {
        return taskRepository.findByGroupId(groupId, pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse create(TaskRequest request, Long creatorId) {
        CreateTaskRequest createReq = new CreateTaskRequest();
        createReq.setGroupId(request.getGroupId());
        createReq.setTitle(request.getTitle());
        createReq.setDescription(request.getDescription());
        createReq.setDueDate(request.getDueDate() != null ? request.getDueDate().atTime(23, 59, 59) : null);
        createReq.setAssigneeId(request.getAssigneeId());
        return createTask(createReq, creatorId);
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, TaskRequest request, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task", id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate() != null ? request.getDueDate().atTime(23, 59, 59) : null);

        task = taskRepository.save(task);
        searchService.indexTask(task);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(taskMapper::toResponse);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task", id));
        task.setStatus(status);
        taskRepository.save(task);
        searchService.indexTask(task);
        log.info("Updated status of task {} to {}", id, status);
    }

    @Override
    @Transactional
    public void addUpdate(Long id, TaskUpdateRequest request, Long internId) {
        UpdateTaskProgressRequest req = new UpdateTaskProgressRequest();
        req.setProgressPercent(request.getProgressPercent());
        req.setContent(request.getContent());
        updateTaskProgress(id, req, internId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskUpdate> getTaskUpdates(Long taskId) {
        return taskUpdateRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new NotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
        searchService.deleteTask(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return getTaskById(id);
    }

    @Override
    @Transactional
    public TaskUpdateDto internCreateUpdate(Long taskId, Long userId, TaskUpdateRequest req) {
        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile", userId));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        if (task.getAssignee() == null || !task.getAssignee().getId().equals(intern.getId())) {
            throw new com.holaho.intern.shared.exception.ForbiddenException("Bạn không được giao task này");
        }

        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setIntern(intern);
        update.setProgressPercent(req.getProgressPercent());
        update.setContent(req.getContent());
        update = taskUpdateRepository.save(update);

        task.setProgressPercent(req.getProgressPercent());
        if (req.getProgressPercent() >= 100) {
            task.setStatus(TaskStatus.DONE);
        } else if (req.getProgressPercent() > 0) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        taskRepository.save(task);
        searchService.indexTask(task);

        return taskUpdateMapper.toDto(update);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskUpdateDto> internListMyTaskUpdates(Long taskId, Long userId, Pageable pageable) {
        internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile", userId));

        return taskUpdateRepository.findByTaskId(taskId, pageable)
                .map(taskUpdateMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskUpdateDto> mentorListTaskUpdates(Long taskId, Long userId, Pageable pageable) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        if (!task.getCreatedBy().getId().equals(userId)) {
            boolean isGroupMentor = task.getGroup().getMentorId() != null
                    && task.getGroup().getMentorId().equals(userId);
            if (!isGroupMentor) {
                throw new ForbiddenException("Bạn không có quyền xem báo cáo của task này");
            }
        }

        return taskUpdateRepository.findByTaskId(taskId, pageable)
                .map(taskUpdateMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByGroup(Long groupId, Pageable pageable) {
        return getTasksByGroupId(groupId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyAssignedTasks(Long internId, Pageable pageable) {
        return taskRepository.findByAssignee_Id(internId, pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByAssignee(Long assigneeId, Pageable pageable) {
        return getMyAssignedTasks(assigneeId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAssignedTasks(Long creatorId, Long assigneeId, Long groupId, String status,
            String keyword, Pageable pageable) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (creatorId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), creatorId));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }
            if (status != null && !status.isEmpty()) {
                try {
                    TaskStatus taskStatus = TaskStatus.valueOf(status);
                    predicates.add(cb.equal(root.get("status"), taskStatus));
                } catch (IllegalArgumentException e) {
                }
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable)
                .map(taskMapper::toResponse);
    }
}

