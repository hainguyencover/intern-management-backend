package com.holaho.intern.service;

import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.entity.Program;
import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.shared.enums.GroupStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramGroupServiceTest {

    @Mock
    private ProgramGroupRepository groupRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private InternProfileRepository internProfileRepository;

    @InjectMocks
    private ProgramGroupService groupService;

    @Test
    void assignIntern_Success() {
        // Arrange
        Long groupId = 1L;
        Long internId = 10L;

        ProgramGroup group = new ProgramGroup();
        group.setId(groupId);
        Program program = new Program();
        program.setStartDate(LocalDate.now());
        group.setProgram(program);

        InternProfile intern = new InternProfile();
        intern.setId(internId);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(internProfileRepository.findById(internId)).thenReturn(Optional.of(intern));
        when(groupMemberRepository.existsByGroupIdAndInternId(groupId, internId)).thenReturn(false);

        // Act
        groupService.assignIntern(groupId, internId);

        // Assert
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(internProfileRepository).save(intern);
        assertEquals(program.getStartDate(), intern.getStartDate());
    }

    @Test
    void assignIntern_Fail_AlreadyAssigned() {
        // Arrange
        when(groupRepository.findById(1L)).thenReturn(Optional.of(new ProgramGroup()));
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(new InternProfile()));
        when(groupMemberRepository.existsByGroupIdAndInternId(1L, 10L)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> groupService.assignIntern(1L, 10L));
    }

    @Test
    void validateMentorSchedule_ConflictDetected() {
        // Arrange
        Long mentorId = 5L;
        String workDays = "Monday,Wednesday";
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(11, 0);

        Program currentProgram = new Program();
        currentProgram.setStartDate(LocalDate.of(2024, 1, 1));
        currentProgram.setEndDate(LocalDate.of(2024, 6, 1));

        ProgramGroup existingGroup = new ProgramGroup();
        existingGroup.setName("Existing Group");
        existingGroup.setWorkDays("Wednesday,Friday"); // Overlaps on Wednesday
        existingGroup.setWorkStartTime(LocalTime.of(10, 0)); // Overlaps 10:00 - 11:00
        existingGroup.setWorkEndTime(LocalTime.of(12, 0));
        Program p2 = new Program();
        p2.setName("P2");
        p2.setStartDate(LocalDate.of(2024, 2, 1));
        p2.setEndDate(LocalDate.of(2024, 4, 1)); // Overlaps dates
        existingGroup.setProgram(p2);

        when(groupRepository.findByMentorIdAndStatus(mentorId, GroupStatus.ACTIVE))
                .thenReturn(List.of(existingGroup));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> groupService.create(new com.holaho.intern.shared.dto.request.GroupRequest(
                        1L, "New Group", 1L, mentorId, start, end, workDays)));
        assertTrue(ex.getMessage().contains("Mentor đã có lịch dạy"));
    }

    @Test
    void delete_Success() {
        // Arrange
        when(groupRepository.existsById(1L)).thenReturn(true);

        // Act
        groupService.delete(1L);

        // Assert
        verify(groupRepository).deleteById(1L);
    }

    @Test
    void delete_Fail_NotFound() {
        // Arrange
        when(groupRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(NotFoundException.class, () -> groupService.delete(1L));
    }
}
