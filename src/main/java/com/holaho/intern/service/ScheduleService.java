package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.shared.dto.response.ScheduleEventResponse;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final InternProfileRepository internProfileRepository;
    private final TaskRepository taskRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public List<ScheduleEventResponse> getMySchedule(String email) {

        InternProfile intern = internProfileRepository.findByUser_Email(email)
                .orElseThrow(() -> new NotFoundException(
                        "Intern profile not found for user: " + email));

        List<ScheduleEventResponse> events = new ArrayList<>();

        // 1. Program Timeline events (if assigned to an active group/program)
        List<GroupMember> memberships = groupMemberRepository.findActiveGroupMembershipsWithProgram(intern.getId());
        if (!memberships.isEmpty() && memberships.get(0).getGroup() != null && memberships.get(0).getGroup().getProgram() != null) {
            Program program = memberships.get(0).getGroup().getProgram();
            if (program.getStartDate() != null) {
                events.add(ScheduleEventResponse.builder()
                        .id(program.getId())
                        .title("Khởi động: " + program.getName())
                        .date(program.getStartDate().toString())
                        .type("PROGRAM_START")
                        .status(program.getStatus().name())
                        .build());
            }
            if (program.getEndDate() != null) {
                events.add(ScheduleEventResponse.builder()
                        .id(program.getId())
                        .title("Báo cáo & Tổng kết: " + program.getName())
                        .date(program.getEndDate().toString())
                        .type("PROGRAM_END")
                        .status(program.getStatus().name())
                        .build());
            }
        }

        // 2. Fetch tasks assigned to the intern via group membership
        List<Task> tasks = taskRepository.findByInternId(intern.getId());

        tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .forEach(task -> events.add(ScheduleEventResponse.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .date(task.getDueDate().toLocalDate().toString())
                        .type("TASK")
                        .status(task.getStatus().name())
                        .build()));

        // 3. Sort chronologically by date
        events.sort(Comparator.comparing(ScheduleEventResponse::getDate));

        return events;
    }
}

