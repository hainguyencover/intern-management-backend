package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.entity.Mentor;
import com.holaho.intern.entity.Task;
import com.holaho.intern.repository.TaskRepository;
import com.holaho.intern.entity.User;
import com.holaho.intern.repository.UserRepository;


import com.holaho.intern.shared.dto.GroupInternDto;
import com.holaho.intern.shared.dto.TaskDto;
import com.holaho.intern.shared.dto.request.CreateMentorTaskRequest;
import com.holaho.intern.shared.dto.response.CreateMentorTaskResponse;
import com.holaho.intern.shared.enums.TaskStatus;
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
    private final UserRepository userRepository; // nÃƒÂ¡Ã‚ÂºÃ‚Â¿u project bÃƒÂ¡Ã‚ÂºÃ‚Â¡n Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ cÃƒÆ’Ã‚Â³ UserRepository

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BÃƒÂ¡Ã‚ÂºÃ‚Â¡n khÃƒÆ’Ã‚Â´ng phÃƒÂ¡Ã‚ÂºÃ‚Â£i mentor cÃƒÂ¡Ã‚Â»Ã‚Â§a group nÃƒÆ’Ã‚Â y");
        }

        User creator = userRepository.findById(mentorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor user not found"));

        List<Long> createdIds = new ArrayList<>();

        for (Long internId : req.internIds()) {
            boolean isMember = groupMemberRepository.existsActiveInGroup((group.getId()), internId);
            if (!isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intern " + internId + " khÃƒÆ’Ã‚Â´ng thuÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢c group");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BÃƒÂ¡Ã‚ÂºÃ‚Â¡n khÃƒÆ’Ã‚Â´ng phÃƒÂ¡Ã‚ÂºÃ‚Â£i mentor cÃƒÂ¡Ã‚Â»Ã‚Â§a group nÃƒÆ’Ã‚Â y");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "BÃƒÂ¡Ã‚ÂºÃ‚Â¡n khÃƒÆ’Ã‚Â´ng phÃƒÂ¡Ã‚ÂºÃ‚Â£i mentor cÃƒÂ¡Ã‚Â»Ã‚Â§a group nÃƒÆ’Ã‚Â y");
        }

        List<GroupMember> members = groupMemberRepository.findActiveMembersByGroupId(groupId);

        return members.stream().map(m -> {
            InternProfile ip = m.getIntern();
            User u = ip.getUser();
            return new GroupInternDto(
                    ip.getId(),
                    u.getId(),
                    u.getFullName(), // nÃƒÂ¡Ã‚ÂºÃ‚Â¿u User bÃƒÂ¡Ã‚ÂºÃ‚Â¡n cÃƒÆ’Ã‚Â³ field fullName; nÃƒÂ¡Ã‚ÂºÃ‚Â¿u khÃƒÆ’Ã‚Â¡c thÃƒÆ’Ã‚Â¬ sÃƒÂ¡Ã‚Â»Ã‚Â­a lÃƒÂ¡Ã‚ÂºÃ‚Â¡i
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
            if (a.getUser() != null) name = a.getUser().getFullName(); // sÃƒÂ¡Ã‚Â»Ã‚Â­a theo field thÃƒÂ¡Ã‚ÂºÃ‚Â­t
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

