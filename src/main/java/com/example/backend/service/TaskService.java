package com.example.backend.service;

import com.example.backend.dto.request.CreateTaskRequest;
import com.example.backend.dto.request.UpdateTaskProgressRequest;
import com.example.backend.dto.response.TaskResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.TaskStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskUpdateRepository taskUpdateRepository;
    private final ProgramGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long creatorId) {
        ProgramGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found: " + request.getGroupId()));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found: " + creatorId));

        Task task = new Task();
        task.setGroup(group);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setCreatedBy(creator);

        if (request.getAssigneeId() != null) {
            InternProfile assignee = internProfileRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found: " + request.getAssigneeId()));
            task.setAssignee(assignee);
        }

        task = taskRepository.save(task);
        log.info("Created task: {}", task.getTitle());

        // Send notification email
        if (task.getAssignee() != null && task.getAssignee().getUser().getEmail() != null) {
            emailService.sendTaskAssignmentEmail(
                    task.getAssignee().getUser().getEmail(),
                    task.getAssignee().getUser().getFullName(),
                    task.getTitle(),
                    task.getDueDate() != null ? task.getDueDate().toString() : null,
                    creator.getFullName());

            // Create in-app notification
            notificationService.createNotification(
                    task.getAssignee().getUser().getId(),
                    com.example.backend.enums.NotificationType.TASK,
                    "Bạn được giao công việc mới: " + task.getTitle(),
                    "Người giao: " + creator.getFullName() + ". Hạn chót: "
                            + (task.getDueDate() != null ? task.getDueDate().toString() : "Không có"));
        }

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskProgress(Long taskId, UpdateTaskProgressRequest request, Long internId) {
        Task task = taskRepository.findByIdWithGroup(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new RuntimeException("Intern not found: " + internId));

        // Create task update
        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setIntern(intern);
        update.setProgressPercent(request.getProgressPercent());
        update.setContent(request.getContent());

        taskUpdateRepository.save(update);

        // Update task status based on progress
        task.setProgressPercent(request.getProgressPercent());
        if (request.getProgressPercent() >= 100) {
            task.setStatus(TaskStatus.DONE);
        } else if (request.getProgressPercent() > 0) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        task = taskRepository.save(task);
        log.info("Updated task {} progress to {}%", taskId, request.getProgressPercent());

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findByIdWithGroup(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByGroupId(Long groupId, Pageable pageable) {
        return taskRepository.findByGroupId(groupId, pageable)
                .map(this::mapToResponse);
    }

    // @Transactional(readOnly = true)
    // public Page<TaskResponse> getTasksByCreator(Long creatorId, Pageable
    // pageable) {
    // return taskRepository.findByCreatedById(creatorId).stream()
    // .collect(Collectors.collectingAndThen(
    // Collectors.toList(),
    // list ->
    // PageImpl.<TaskResponse>of(list.stream().map(this::mapToResponse).collect(Collectors.toList()),
    // pageable, list.size())
    // ));
    // }

    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse create(com.example.backend.dto.request.TaskRequest request, Long creatorId) {
        CreateTaskRequest createReq = new CreateTaskRequest();
        createReq.setGroupId(request.getGroupId());
        createReq.setTitle(request.getTitle());
        createReq.setDescription(request.getDescription());
        createReq.setDueDate(request.getDueDate() != null ? request.getDueDate().atTime(23, 59, 59) : null);
        createReq.setAssigneeId(request.getAssigneeId());
        return createTask(createReq, creatorId);
    }

    @Transactional
    public TaskResponse update(Long id, com.example.backend.dto.request.TaskRequest request, Long userId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task", id));

        // TODO: check permission?

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate() != null ? request.getDueDate().atTime(23, 59, 59) : null);

        task = taskRepository.save(task);
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        return taskRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional
    public void updateStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task", id));
        task.setStatus(status);
        taskRepository.save(task);
        log.info("Updated status of task {} to {}", id, status);
    }

    @Transactional
    public void addUpdate(Long id, com.example.backend.dto.request.TaskUpdateRequest request, Long internId) {
        UpdateTaskProgressRequest req = new UpdateTaskProgressRequest();
        req.setProgressPercent(request.getProgressPercent());
        req.setContent(request.getContent());
        updateTaskProgress(id, req, internId);
    }

    @Transactional(readOnly = true)
    public List<TaskUpdate> getTaskUpdates(Long taskId) {
        // Assuming there is a method in repo
        return taskUpdateRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new NotFoundException("Task", id);
        }
        taskRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long id) {
        return getTaskById(id);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByGroup(Long groupId, Pageable pageable) {
        return getTasksByGroupId(groupId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getMyAssignedTasks(Long internId, Pageable pageable) {
        return taskRepository.findByAssignee_Id(internId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByAssignee(Long assigneeId, Pageable pageable) {
        return getMyAssignedTasks(assigneeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAssignedTasks(Long creatorId, Long assigneeId, Long groupId, String status,
            String keyword, Pageable pageable) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();

            // Filter by creator (Mentor)
            if (creatorId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), creatorId));
            }

            // Filter by assignee
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }

            // Filter by group
            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                try {
                    TaskStatus taskStatus = TaskStatus.valueOf(status);
                    predicates.add(cb.equal(root.get("status"), taskStatus));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status or handle error
                }
            }

            // Keyword search
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern),
                        cb.like(cb.lower(root.get("description")), likePattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .groupId(task.getGroup().getId())
                .groupName(task.getGroup().getName())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .status(task.getStatus())
                .createdBy(task.getCreatedBy().getId())
                .creatorName(task.getCreatedBy().getFullName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getUser().getFullName() : null)
                .progressPercent(task.getProgressPercent())
                .createdAt(task.getCreatedAt())
                .build();
    }
}
