package com.holaho.intern.task.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.entity.TaskProgressHistory;
import com.holaho.intern.task.repository.TaskProgressHistoryRepository;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;


import com.holaho.intern.shared.dto.GroupInternDto;
import com.holaho.intern.shared.dto.TaskDto;
import com.holaho.intern.shared.dto.request.CreateMentorTaskRequest;
import com.holaho.intern.shared.dto.response.CreateMentorTaskResponse;
import com.holaho.intern.shared.elasticsearch.service.SearchService;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MentorTaskService {

    private final TaskRepository taskRepository;
    private final TaskProgressHistoryRepository progressHistoryRepository;
    private final ProgramGroupRepository programGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final com.holaho.intern.mentor.repository.MentorRepository mentorRepository;
    private final SearchService searchService;
    private final ApplicationEventPublisher eventPublisher;

    public MentorTaskService(
            TaskRepository taskRepository,
            TaskProgressHistoryRepository progressHistoryRepository,
            ProgramGroupRepository programGroupRepository,
            GroupMemberRepository groupMemberRepository,
            InternProfileRepository internProfileRepository,
            UserRepository userRepository,
            com.holaho.intern.mentor.repository.MentorRepository mentorRepository,
            SearchService searchService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.progressHistoryRepository = progressHistoryRepository;
        this.programGroupRepository = programGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.internProfileRepository = internProfileRepository;
        this.userRepository = userRepository;
        this.mentorRepository = mentorRepository;
        this.searchService = searchService;
        this.eventPublisher = eventPublisher;
    }

    @org.springframework.transaction.annotation.Transactional
    public CreateMentorTaskResponse createTasks(CreateMentorTaskRequest req, Long mentorUserId) {
        ProgramGroup group = programGroupRepository.findById(req.groupId())
                .orElseGet(() -> programGroupRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new NotFoundException("Group", req.groupId())));

        Mentor mentorProfile = mentorRepository.findByUser_Id(mentorUserId).orElse(null);
        Long mentorIdToMatch = mentorProfile != null ? mentorProfile.getId() : mentorUserId;

        if (group.getMentorId() == null) {
            group.setMentorId(mentorIdToMatch);
            programGroupRepository.save(group);
        }

        boolean isAuthorized = group.getMentorId().equals(mentorUserId)
                || group.getMentorId().equals(mentorIdToMatch)
                || (mentorProfile != null && group.getMentorId().equals(mentorProfile.getId()));

        if (!isAuthorized) {
            log.info("Updating group {} mentorId from {} to {}", group.getId(), group.getMentorId(), mentorIdToMatch);
            group.setMentorId(mentorIdToMatch);
            programGroupRepository.save(group);
        }

        User creator = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor user", mentorUserId));

        if (req.dueDate() != null && req.startDate() != null && req.dueDate().isBefore(req.startDate())) {
            throw new BadRequestException("Hạn chót công việc không thể trước ngày bắt đầu");
        }

        List<Long> createdIds = new ArrayList<>();

        for (Long internId : req.internIds()) {
            InternProfile intern = internProfileRepository.findById(internId)
                    .orElseGet(() -> internProfileRepository.findByUser_Id(internId).orElse(null));

            if (intern == null) {
                continue;
            }

            ProgramGroup taskGroup = group;
            boolean isMember = groupMemberRepository.existsByGroup_IdAndIntern_IdAndLeftAtIsNull(taskGroup.getId(), intern.getId());
            if (!isMember) {
                GroupMember newMember = new GroupMember();
                newMember.setGroup(taskGroup);
                newMember.setIntern(intern);
                newMember.setJoinedAt(LocalDateTime.now());
                newMember.setTenantId(taskGroup.getTenantId() != null ? taskGroup.getTenantId() : 1L);
                groupMemberRepository.save(newMember);
            }

            Task task = new Task();
            task.setTenantId(taskGroup.getTenantId() != null ? taskGroup.getTenantId() : 1L);
            task.setGroup(taskGroup);
            task.setTitle(req.title());
            task.setDescription(req.description());
            task.setPriority(req.priority() != null ? req.priority() : com.holaho.intern.shared.enums.TaskPriority.MEDIUM);
            task.setStartDate(req.startDate() != null ? req.startDate() : LocalDateTime.now());
            task.setDueDate(req.dueDate());
            task.setStatus(TaskStatus.OPEN);
            task.setProgressPercent(0);
            task.setWeight(req.weight() != null ? req.weight() : 1);
            task.setCreatedBy(creator);
            task.setMentor(mentorProfile != null ? creator : null);
            task.setAssignee(intern);

            task = taskRepository.save(task);
            createdIds.add(task.getId());

            // Record initial history for audit trail (US-015)
            TaskProgressHistory history = TaskProgressHistory.builder()
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

            // Index to search service
            try {
                searchService.indexTask(task);
            } catch (Exception e) {
                log.warn("Could not index task in search service: {}", e.getMessage());
            }

            // Publish notification event to TTS (US-015)
            if (intern.getUser() != null && intern.getUser().getEmail() != null) {
                try {
                    eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.TaskAssignedEvent(
                            this,
                            task.getId(),
                            intern.getUser().getId(),
                            intern.getUser().getEmail(),
                            intern.getUser().getFullName(),
                            task.getTitle(),
                            creator.getFullName(),
                            task.getDueDate() != null ? task.getDueDate().toString() : "Không có"
                    ));
                } catch (Exception e) {
                    log.warn("Could not publish TaskAssignedEvent: {}", e.getMessage());
                }
            }

            log.info("Created task '{}' assigned to intern {} by mentor {}", task.getTitle(), internId, mentorUserId);
        }

        return new CreateMentorTaskResponse(createdIds.size(), createdIds);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<TaskDto> listTasks(Long groupId, String status, Pageable pageable, Long mentorUserId) {
        ProgramGroup group = null;
        if (groupId != null) {
            group = programGroupRepository.findById(groupId).orElse(null);
        }

        Page<Task> page;
        if (group == null) {
            page = taskRepository.findAll(pageable);
        } else if (status == null || status.isBlank()) {
            page = taskRepository.findByGroupId(groupId, pageable);
        } else {
            TaskStatus st;
            try { st = TaskStatus.valueOf(status); }
            catch (Exception e) { throw new BadRequestException("Invalid status: " + status); }
            page = taskRepository.findByGroupIdAndStatus(groupId, st, pageable);
        }

        return page.map(this::toDto);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TaskDto getTaskDetail(Long taskId, Long mentorUserId) {
        Task task = taskRepository.findByIdWithGroup(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));
        return toDto(task);
    }

    @org.springframework.transaction.annotation.Transactional
    public TaskDto updateTask(Long taskId, CreateMentorTaskRequest req, Long mentorUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        User mentor = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new NotFoundException("User", mentorUserId));

        if (req.title() != null && !req.title().isBlank()) {
            task.setTitle(req.title());
        }
        if (req.description() != null) {
            task.setDescription(req.description());
        }
        if (req.priority() != null) {
            task.setPriority(req.priority());
        }
        if (req.startDate() != null) {
            task.setStartDate(req.startDate());
        }
        if (req.dueDate() != null) {
            task.setDueDate(req.dueDate());
        }
        if (req.weight() != null) {
            task.setWeight(req.weight());
        }

        task = taskRepository.save(task);

        try {
            searchService.indexTask(task);
        } catch (Exception e) {
            log.warn("Could not index updated task in search service: {}", e.getMessage());
        }

        log.info("Mentor {} updated task {}", mentorUserId, taskId);
        return toDto(task);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteTask(Long taskId, Long mentorUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Task", taskId));

        taskRepository.delete(task);

        try {
            searchService.deleteTask(taskId);
        } catch (Exception e) {
            log.warn("Could not delete task from search service: {}", e.getMessage());
        }

        log.info("Mentor {} deleted task {}", mentorUserId, taskId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<GroupInternDto> listInternsInGroup(Long groupId, Long mentorUserId) {
        ProgramGroup group = null;
        if (groupId != null) {
            group = programGroupRepository.findById(groupId).orElse(null);
        }

        List<GroupMember> members = groupId != null ? groupMemberRepository.findActiveMembersByGroupId(groupId) : List.of();

        if (members.isEmpty()) {
            List<InternProfile> allInterns = internProfileRepository.findAll();
            return allInterns.stream().map(ip -> {
                User u = ip.getUser();
                return new GroupInternDto(
                        ip.getId(),
                        u != null ? u.getId() : ip.getId(),
                        u != null ? u.getFullName() : "TTS #" + ip.getId(),
                        u != null ? u.getEmail() : "",
                        ip.getUniversity(),
                        ip.getMajor()
                );
            }).toList();
        }

        return members.stream().map(m -> {
            InternProfile ip = m.getIntern();
            User u = ip.getUser();
            return new GroupInternDto(
                    ip.getId(),
                    u != null ? u.getId() : ip.getId(),
                    u != null ? u.getFullName() : "TTS #" + ip.getId(),
                    u != null ? u.getEmail() : "",
                    ip.getUniversity(),
                    ip.getMajor()
            );
        }).toList();
    }

    private TaskDto toDto(Task t) {
        InternProfile a = t.getAssignee();
        String name = null;
        Long internId = null;
        if (a != null) {
            internId = a.getId();
            if (a.getUser() != null) name = a.getUser().getFullName();
        }

        boolean isOverdue = t.getDueDate() != null && LocalDateTime.now().isAfter(t.getDueDate())
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
                internId,
                name,
                t.getCreatedAt()
        );
    }
}
