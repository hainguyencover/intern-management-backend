package com.holaho.intern.service;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.shared.dto.request.CreateProgramRequest;
import com.holaho.intern.shared.dto.response.ProgramResponse;
import com.holaho.intern.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProgramGroupRepository groupRepository;

    @Mock
    private GroupMemberRepository memberRepository;

    @InjectMocks
    private ProgramService programService;

    private Department mockDept;

    @BeforeEach
    void setUp() {
        mockDept = new Department();
        mockDept.setId(1L);
        mockDept.setName("Engineering");
    }

    @Test
    @DisplayName("Should create program with valid code, dates, and max interns")
    void shouldCreateProgramWithUniqueCode() {
        CreateProgramRequest request = new CreateProgramRequest();
        request.setDepartmentId(1L);
        request.setCode("BE-2026");
        request.setName("Backend Java Program 2026");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 8, 31));
        request.setMaxInterns(20);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDept));
        when(programRepository.existsByCode("BE-2026")).thenReturn(false);
        when(programRepository.save(any(Program.class))).thenAnswer(invocation -> {
            Program p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProgramResponse response = programService.createProgram(request);

        assertNotNull(response);
        assertEquals("BE-2026", response.getCode());
        assertEquals("Backend Java Program 2026", response.getName());
        assertEquals(20, response.getMaxInterns());
        verify(programRepository, times(1)).save(any(Program.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when start date is after end date")
    void shouldRejectInvalidDateRange() {
        CreateProgramRequest request = new CreateProgramRequest();
        request.setDepartmentId(1L);
        request.setName("Invalid Dates Program");
        request.setStartDate(LocalDate.of(2026, 9, 1));
        request.setEndDate(LocalDate.of(2026, 8, 1));

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDept));

        assertThrows(BadRequestException.class, () -> programService.createProgram(request));
        verify(programRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when code is duplicate")
    void shouldRejectDuplicateCode() {
        CreateProgramRequest request = new CreateProgramRequest();
        request.setDepartmentId(1L);
        request.setCode("DUP-01");
        request.setName("Duplicate Code Program");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 8, 31));

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mockDept));
        when(programRepository.existsByCode("DUP-01")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> programService.createProgram(request));
        verify(programRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-013: Should successfully update timeline when no active interns exist")
    void shouldUpdateProgramTimelineSuccessfully() {
        Program program = new Program();
        program.setId(1L);
        program.setName("Test Program");
        program.setStartDate(LocalDate.of(2026, 6, 1));
        program.setEndDate(LocalDate.of(2026, 8, 31));
        program.setStatus(com.holaho.intern.shared.enums.ProgramStatus.ACTIVE);

        when(programRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(program));
        when(memberRepository.countByGroup_ProgramId(1L)).thenReturn(0L);
        when(programRepository.save(any(Program.class))).thenAnswer(i -> i.getArgument(0));

        com.holaho.intern.shared.dto.request.UpdateProgramTimelineRequest req =
                new com.holaho.intern.shared.dto.request.UpdateProgramTimelineRequest(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 11, 30)
                );

        ProgramResponse response = programService.updateProgramTimeline(1L, req);

        assertNotNull(response);
        assertEquals(LocalDate.of(2026, 9, 1), response.getStartDate());
        assertEquals(LocalDate.of(2026, 11, 30), response.getEndDate());
    }

    @Test
    @DisplayName("US-013: Should throw ConflictException when active interns exist")
    void shouldThrowConflictWhenActiveInternsExist() {
        Program program = new Program();
        program.setId(1L);
        program.setName("Active Program");
        program.setStartDate(LocalDate.of(2026, 6, 1));
        program.setEndDate(LocalDate.of(2026, 8, 31));
        program.setStatus(com.holaho.intern.shared.enums.ProgramStatus.ACTIVE);

        when(programRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(program));
        when(memberRepository.countByGroup_ProgramId(1L)).thenReturn(5L);

        com.holaho.intern.shared.dto.request.UpdateProgramTimelineRequest req =
                new com.holaho.intern.shared.dto.request.UpdateProgramTimelineRequest(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 11, 30)
                );

        assertThrows(com.holaho.intern.shared.exception.ConflictException.class,
                () -> programService.updateProgramTimeline(1L, req));
    }

    @Test
    @DisplayName("US-015: Should return detailed program timeline info")
    void shouldGetProgramTimelineDetails() {
        Program program = new Program();
        program.setId(1L);
        program.setName("Java Internship");
        program.setCode("JAVA-01");
        program.setStartDate(LocalDate.of(2026, 9, 1));
        program.setEndDate(LocalDate.of(2026, 11, 30));
        program.setStatus(com.holaho.intern.shared.enums.ProgramStatus.ACTIVE);
        program.setDepartment(mockDept);

        when(programRepository.findByIdWithDepartment(1L)).thenReturn(Optional.of(program));
        when(memberRepository.countByGroup_ProgramId(1L)).thenReturn(12L);

        com.holaho.intern.shared.dto.response.ProgramTimelineResponse res = programService.getProgramTimelineDetails(1L);

        assertNotNull(res);
        assertEquals(1L, res.getProgramId());
        assertEquals("Java Internship", res.getProgramName());
        assertEquals("JAVA-01", res.getProgramCode());
        assertEquals(90L, res.getDurationDays());
        assertEquals(12L, res.getInternCount());
    }
}
