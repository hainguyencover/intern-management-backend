package com.example.backend.service;

import com.example.backend.dto.GroupInternDto;
import com.example.backend.dto.TaskDto;
import com.example.backend.dto.request.CreateMentorTaskRequest;
import com.example.backend.dto.response.CreateMentorTaskResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.TaskStatus;
import com.example.backend.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class MentorTaskService {

    private final TaskRepository taskRepository;
    private final ProgramGroupRepository programGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository; // nếu project bạn đã có UserRepository

    public MentorTaskService(
            TaskRepository taskRepository,
            ProgramGroupRepository programGroupRepository,
            GroupMemberRepository groupMemberRepository,
            InternProfileRepository internProfileRepository,
            UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.programGroupRepository = programGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.internProfileRepository = internProfileRepository;
        this.userRepository = userRepository;
    }

    public CreateMentorTaskResponse createTasks(CreateMentorTaskRequest req, Long mentorUserId) {
        ProgramGroup group = programGroupRepository.findById(req.groupId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.getMentorId() == null || !group.getMentorId().equals(mentorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải mentor của group này");
        }

        User creator = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor user not found"));

        List<Long> createdIds = new ArrayList<>();

        for (Long internId : req.internIds()) {
            boolean isMember = groupMemberRepository.existsActiveInGroup((group.getId()), internId);
            if (!isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intern " + internId + " không thuộc group");
            }

            InternProfile intern = internProfileRepository.findById(internId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "InternProfile not found: " + internId));

            Task task = new Task();
            task.setGroup(group);
            task.setTitle(req.title());
            task.setDescription(req.description());
            task.setDueDate(req.dueDate());
            task.setStatus(TaskStatus.OPEN);
            task.setCreatedBy(creator);
            task.setAssignee(intern);

            createdIds.add(taskRepository.save(task).getId());
        }

        return new CreateMentorTaskResponse(createdIds.size(), createdIds);
    }

    public Page<TaskDto> listTasks(Long groupId, String status, Pageable pageable, Long mentorUserId) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.getMentorId() == null || !group.getMentorId().equals(mentorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải mentor của group này");
        }

        Page<Task> page;
        if (status == null || status.isBlank()) {
            page = taskRepository.findByGroupId(groupId, pageable);
        } else {
            TaskStatus st;
            try { st = TaskStatus.valueOf(status); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status"); }
            page = taskRepository.findByCreatedByIdAndStatus(groupId, st, pageable);
        }

        return page.map(this::toDto);
    }

    public List<GroupInternDto> listInternsInGroup(Long groupId, Long mentorUserId) {
        ProgramGroup group = programGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (group.getMentorId() == null || !group.getMentorId().equals(mentorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải mentor của group này");
        }

        List<GroupMember> members = groupMemberRepository.findActiveMembersByGroupId(groupId);

        return members.stream().map(m -> {
            InternProfile ip = m.getIntern();
            User u = ip.getUser();
            return new GroupInternDto(
                    ip.getId(),
                    u.getId(),
                    u.getFullName(), // nếu User bạn có field fullName; nếu khác thì sửa lại
                    u.getEmail(),
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
            if (a.getUser() != null) name = a.getUser().getFullName(); // sửa theo field thật
        }

        return new TaskDto(
                t.getId(),
                t.getGroup() != null ? t.getGroup().getId() : null,
                t.getTitle(),
                t.getDescription(),
                t.getDueDate(),
                t.getStatus(),
                internId,
                name
        );
    }
}
