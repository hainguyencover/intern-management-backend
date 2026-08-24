package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.shared.dto.response.ScheduleEventResponse;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private InternProfile mockIntern;
    private Program mockProgram;
    private ProgramGroup mockGroup;
    private GroupMember mockMember;

    @BeforeEach
    void setUp() {
        mockIntern = new InternProfile();
        mockIntern.setId(10L);

        mockProgram = new Program();
        mockProgram.setId(1L);
        mockProgram.setName("Java Program");
        mockProgram.setStartDate(LocalDate.of(2026, 9, 1));
        mockProgram.setEndDate(LocalDate.of(2026, 11, 30));
        mockProgram.setStatus(ProgramStatus.ACTIVE);

        mockGroup = new ProgramGroup();
        mockGroup.setId(100L);
        mockGroup.setProgram(mockProgram);

        mockMember = new GroupMember();
        mockMember.setId(1000L);
        mockMember.setIntern(mockIntern);
        mockMember.setGroup(mockGroup);
    }

    @Test
    @DisplayName("US-014 & US-018: Should return aggregated schedule with Program boundaries and assigned Tasks")
    void shouldReturnAggregatedSchedule() {
        String email = "intern@test.com";
        when(internProfileRepository.findByUser_Email(email)).thenReturn(Optional.of(mockIntern));
        when(groupMemberRepository.findActiveGroupMembershipsWithProgram(10L)).thenReturn(List.of(mockMember));

        Task task1 = new Task();
        task1.setId(50L);
        task1.setTitle("Authentication API");
        task1.setDueDate(LocalDateTime.of(2026, 9, 15, 17, 0));
        task1.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByInternId(10L)).thenReturn(List.of(task1));

        List<ScheduleEventResponse> schedule = scheduleService.getMySchedule(email);

        assertNotNull(schedule);
        assertEquals(3, schedule.size()); // PROGRAM_START, TASK, PROGRAM_END sorted by date

        assertEquals("PROGRAM_START", schedule.get(0).getType());
        assertEquals("2026-09-01", schedule.get(0).getDate());

        assertEquals("TASK", schedule.get(1).getType());
        assertEquals("Authentication API", schedule.get(1).getTitle());
        assertEquals("2026-09-15", schedule.get(1).getDate());

        assertEquals("PROGRAM_END", schedule.get(2).getType());
        assertEquals("2026-11-30", schedule.get(2).getDate());
    }
}
