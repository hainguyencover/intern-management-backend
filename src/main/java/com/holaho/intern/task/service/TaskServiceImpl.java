package com.holaho.intern.task.service;

import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.entity.TaskUpdate;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.task.repository.TaskUpdateRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.exception.BadRequestException;
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
    private final com.holaho.intern.task.repository.TaskProgressHistoryRepository progressHistoryRepository;
    private final ProgramGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final com.holaho.intern.mentor.repository.MentorRepository mentorRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    private final TaskMapper taskMapper;
    private final TaskUpdateMapper taskUpdateMapper;
    private final SearchService searchService;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long creatorId) {
        ProgramGroup group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId()).orElse(null);
        }
        if (group == null) {
            group = groupRepository.findAll().stream().findFirst().orElse(null);
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("User", creatorId));


        if (request.getDueDate() != null && request.getStartDate() != null && request.getDueDate().isBefore(request.getStartDate())) {
            throw new com.holaho.intern.shared.exception.BadRequestException("Hạn chót công việc không thể trước ngày bắt đầu");
        }

        Task task = new Task();
        task.setTenantId(group != null && group.getTenantId() != null ? group.getTenantId() : 1L);
        task.setGroup(group);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : com.holaho.intern.shared.enums.TaskPriority.MEDIUM);
        task.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setProgressPercent(0);
        task.setWeight(request.getWeight() != null ? request.getWeight() : 1);
        task.setCreatedBy(creator);
        
        Mentor mentorProfile = mentorRepository.findByUser_Id(creatorId).orElse(null);
        task.setMentor(mentorProfile != null ? creator : null);

        if (request.getAssigneeId() != null) {
            InternProfile assignee = internProfileRepository.findById(request.getAssigneeId())
                    .orElseGet(() -> internProfileRepository.findByUser_Id(request.getAssigneeId()).orElse(null));
            if (assignee == null) {
                assignee = internProfileRepository.findAll().stream().findFirst().orElse(null);
            }
            task.setAssignee(assignee);
        }

        task = taskRepository.save(task);
        if (entityManager != null) {
            entityManager.flush();
        }

        // Record initial history
        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(null)
                .newProgress(0)
                .oldStatus(null)
                .newStatus(TaskStatus.OPEN)
                .note("Nhiệm vụ được tạo mới: " + task.getTitle())
                .changedBy(creator)
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);
        if (entityManager != null) {
            entityManager.flush();
        }

        try {
            searchService.indexTask(task);
        } catch (Exception e) {
            log.warn("Could not index task in search service: {}", e.getMessage());
        }

        log.info("Created task: {}", task.getTitle());

        if (task.getAssignee() != null && task.getAssignee().getUser() != null && task.getAssignee().getUser().getEmail() != null) {
            try {
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
            } catch (Exception e) {
                log.warn("Could not publish TaskAssignedEvent: {}", e.getMessage());
            }
        }

        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskProgress(Long taskId, UpdateTaskProgressRequest request, Long internId) {
        Task task = taskRepository.findByIdWithGroup(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseGet(() -> internProfileRepository.findByUser_Id(internId)
                        .orElseThrow(() -> new NotFoundException("InternProfile", internId)));

        // BR-016-02: Only update own assigned task or group task
        boolean isAssignee = task.getAssignee() != null && (
                task.getAssignee().getId().equals(intern.getId()) ||
                (task.getAssignee().getUser() != null && intern.getUser() != null && task.getAssignee().getUser().getId().equals(intern.getUser().getId()))
        );
        if (!isAssignee) {
            boolean isGroupMember = task.getGroup() != null && groupMemberRepository.existsByGroup_IdAndIntern_IdAndLeftAtIsNull(task.getGroup().getId(), intern.getId());
            if (!isGroupMember) {
                throw new ForbiddenException("Bạn không có quyền cập nhật tiến độ cho nhiệm vụ này");
            }
        }

        // BR-016-08: Tenant isolation check
        Long currentTenantId = TenantContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(task.getTenantId())) {
            throw new ForbiddenException("Nhiệm vụ không thuộc chi nhánh của bạn");
        }

        // BR-016-06: Task completed cannot be updated
        if (task.getStatus().isCompleted()) {
            throw new BadRequestException("Nhiệm vụ đã hoàn thành không thể chỉnh sửa tiến độ");
        }

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        Integer newProgress = request.getProgressPercent() != null ? request.getProgressPercent() : oldProgress;
        
        // BR-016-05: Progress cannot decrease
        if (newProgress < oldProgress) {
            throw new BadRequestException("Tiến độ công việc không thể giảm từ " + oldProgress + "% xuống " + newProgress + "%");
        }

        // BR-016-04: Automatic status transition
        TaskStatus newStatus;
        if (newProgress == 0) {
            newStatus = TaskStatus.OPEN;
        } else if (newProgress < 100) {
            newStatus = TaskStatus.IN_PROGRESS;
        } else {
            newStatus = TaskStatus.DONE;
        }

        TaskUpdate update = new TaskUpdate();
        update.setTask(task);
        update.setIntern(intern);
        update.setProgressPercent(newProgress);
        update.setContent(request.getContent());
        taskUpdateRepository.save(update);

        task.setProgressPercent(newProgress);
        task.setStatus(newStatus);
        if (newStatus == TaskStatus.DONE) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task = taskRepository.save(task);

        // Save history audit log
        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(newProgress)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(request.getContent() != null && !request.getContent().trim().isEmpty() ? request.getContent() : "Cập nhật tiến độ " + newProgress + "%")
                .changedBy(intern.getUser())
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        if (newStatus == TaskStatus.DONE && task.getCreatedBy() != null) {
            try {
                eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskCompletedEvent(
                        this,
                        task.getId(),
                        task.getCreatedBy().getId(),
                        intern.getUser().getFullName(),
                        task.getTitle()
                ));
            } catch (Exception e) {
                log.warn("Could not publish TaskCompletedEvent: {}", e.getMessage());
            }
        }
        searchService.indexTask(task);
        log.info("Updated task {} progress to {}%", taskId, newProgress);

        return TaskResponse.from(task);
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
        return taskRepository.findAllWithDetails(pageable).map(taskMapper::toResponse);
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

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        task.setProgressPercent(req.getProgressPercent());
        TaskStatus newStatus;
        if (req.getProgressPercent() >= 100) {
            newStatus = TaskStatus.SUBMITTED;
        } else if (req.getProgressPercent() > 0) {
            newStatus = TaskStatus.IN_PROGRESS;
        } else {
            newStatus = oldStatus;
        }
        task.setStatus(newStatus);
        taskRepository.save(task);

        // Record progress history for audit trail
        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(req.getProgressPercent())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(req.getContent() != null ? req.getContent() : "Cập nhật tiến độ " + req.getProgressPercent() + "%")
                .changedBy(intern.getUser())
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        // Notify mentor when task is submitted for review
        if (req.getProgressPercent() >= 100 && task.getCreatedBy() != null) {
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskCompletedEvent(
                    this,
                    task.getId(),
                    task.getCreatedBy().getId(),
                    intern.getUser().getFullName(),
                    task.getTitle()
            ));
        }
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
                .map(TaskResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.holaho.intern.shared.dto.response.TaskProgressHistoryResponse> getProgressHistory(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new NotFoundException("Task", taskId);
        }
        return progressHistoryRepository.findByTaskIdOrderByChangedAtDesc(taskId).stream()
                .map(com.holaho.intern.shared.dto.response.TaskProgressHistoryResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TaskResponse submitTask(Long taskId, Long internUserId, String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile", internUserId)));

        boolean isAssignee = task.getAssignee() != null && (
                task.getAssignee().getId().equals(intern.getId()) ||
                (task.getAssignee().getUser() != null && intern.getUser() != null && task.getAssignee().getUser().getId().equals(intern.getUser().getId()))
        );
        if (!isAssignee) {
            boolean isGroupMember = task.getGroup() != null && groupMemberRepository.existsByGroup_IdAndIntern_IdAndLeftAtIsNull(task.getGroup().getId(), intern.getId());
            if (!isGroupMember) {
                throw new ForbiddenException("Bạn không phải người thực hiện nhiệm vụ này");
            }
        }

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        task.setProgressPercent(100);
        task.setStatus(TaskStatus.DONE);
        task = taskRepository.save(task);

        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(100)
                .oldStatus(oldStatus)
                .newStatus(TaskStatus.DONE)
                .note(note != null && !note.isBlank() ? note : "TTS nộp báo cáo hoàn thành nhiệm vụ")
                .changedBy(intern.getUser())
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        if (task.getCreatedBy() != null) {
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskCompletedEvent(
                    this,
                    task.getId(),
                    task.getCreatedBy().getId(),
                    intern.getUser().getFullName(),
                    task.getTitle()
            ));
        }

        searchService.indexTask(task);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse approveTask(Long taskId, Long mentorUserId, String note) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        User mentorUser = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("User", mentorUserId));

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        task.setStatus(TaskStatus.APPROVED);
        task.setProgressPercent(100);
        task.setCompletedAt(LocalDateTime.now());
        task = taskRepository.save(task);

        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(100)
                .oldStatus(oldStatus)
                .newStatus(TaskStatus.APPROVED)
                .note(note != null && !note.isBlank() ? note : "Mentor phê duyệt hoàn thành nhiệm vụ")
                .changedBy(mentorUser)
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        searchService.indexTask(task);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse rejectTask(Long taskId, Long mentorUserId, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        User mentorUser = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("User", mentorUserId));

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        task.setStatus(TaskStatus.NEEDS_CHANGES);
        task = taskRepository.save(task);

        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(oldProgress)
                .oldStatus(oldStatus)
                .newStatus(TaskStatus.NEEDS_CHANGES)
                .note(reason != null && !reason.isBlank() ? "Yêu cầu làm lại: " + reason : "Mentor yêu cầu chỉnh sửa/làm lại nhiệm vụ")
                .changedBy(mentorUser)
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        searchService.indexTask(task);
        log.info("Task {} rejected by mentor with status NEEDS_CHANGES", taskId);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional
    public TaskResponse cancelTask(Long taskId, Long mentorUserId, String reason) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        User mentorUser = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("User", mentorUserId));

        Integer oldProgress = task.getProgressPercent() != null ? task.getProgressPercent() : 0;
        TaskStatus oldStatus = task.getStatus();

        task.setStatus(TaskStatus.CANCELLED);
        task = taskRepository.save(task);

        com.holaho.intern.task.entity.TaskProgressHistory history = com.holaho.intern.task.entity.TaskProgressHistory.builder()
                .task(task)
                .oldProgress(oldProgress)
                .newProgress(oldProgress)
                .oldStatus(oldStatus)
                .newStatus(TaskStatus.CANCELLED)
                .note(reason != null && !reason.isBlank() ? "Đã hủy task. Lý do: " + reason : "Mentor đã hủy nhiệm vụ này")
                .changedBy(mentorUser)
                .changedAt(LocalDateTime.now())
                .build();
        history.setTenantId(task.getTenantId() != null ? task.getTenantId() : 1L);
        progressHistoryRepository.save(history);

        searchService.indexTask(task);
        log.info("Task {} cancelled by mentor with reason: {}", taskId, reason);
        return TaskResponse.from(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasksForMentor(Long mentorUserId) {
        return taskRepository.findOverdueTasks(LocalDateTime.now()).stream()
                .filter(t -> t.getCreatedBy() != null && t.getCreatedBy().getId().equals(mentorUserId))
                .map(TaskResponse::from)
                .toList();
    }
}



