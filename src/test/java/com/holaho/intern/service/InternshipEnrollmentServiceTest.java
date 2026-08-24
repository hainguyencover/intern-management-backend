package com.holaho.intern.service;

import com.holaho.intern.entity.InternshipEnrollment;
import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.InternshipEnrollmentRepository;
import com.holaho.intern.repository.ProgramGroupRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.shared.dto.response.EnrollmentResponse;
import com.holaho.intern.shared.enums.EnrollmentStatus;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.user.entity.User;
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
class InternshipEnrollmentServiceTest {

    @Mock
    private InternshipEnrollmentRepository enrollmentRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private ProgramGroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;

    @InjectMocks
    private InternshipEnrollmentService enrollmentService;

    private Program mockProgram;
    private InternProfile mockIntern;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockProgram = new Program();
        mockProgram.setId(1L);
        mockProgram.setName("Software Engineering 2026");
        mockProgram.setCode("SE-2026");
        mockProgram.setStatus(ProgramStatus.ACTIVE);
        mockProgram.setMaxInterns(30);

        mockUser = new User();
        mockUser.setId(10L);
        mockUser.setFullName("Nguyen Van Intern");
        mockUser.setEmail("intern@test.com");

        mockIntern = new InternProfile();
        mockIntern.setId(100L);
        mockIntern.setUser(mockUser);
        mockIntern.setStatus("APPROVED");
    }

    @Test
    @DisplayName("US-059: Should enroll intern successfully when program has capacity and intern is active")
    void shouldEnrollApprovedInternSuccessfully() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(mockProgram));
        when(internProfileRepository.findById(100L)).thenReturn(Optional.of(mockIntern));
        when(enrollmentRepository.existsByProgramIdAndInternId(1L, 100L)).thenReturn(false);
        when(enrollmentRepository.findByInternIdAndStatus(100L, EnrollmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(InternshipEnrollment.class))).thenAnswer(inv -> {
            InternshipEnrollment e = inv.getArgument(0);
            e.setId(500L);
            return e;
        });

        EnrollmentResponse response = enrollmentService.enrollIntern(1L, 100L, null, LocalDate.now());

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(1L, response.getProgramId());
        assertEquals(100L, response.getInternId());
        verify(enrollmentRepository, times(1)).save(any(InternshipEnrollment.class));
    }

    @Test
    @DisplayName("US-059: Should throw BadRequestException when trying to enroll into completed program")
    void shouldRejectEnrollmentWhenProgramIsCompleted() {
        mockProgram.setStatus(ProgramStatus.COMPLETED);
        when(programRepository.findById(1L)).thenReturn(Optional.of(mockProgram));

        assertThrows(BadRequestException.class, () ->
                enrollmentService.enrollIntern(1L, 100L, null, LocalDate.now())
        );
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-02: Should throw ConflictException when intern already has an active program enrollment")
    void shouldEnforceSingleActiveProgramConstraint() {
        Program otherProgram = new Program();
        otherProgram.setId(2L);
        otherProgram.setName("Frontend Vue 2026");

        InternshipEnrollment activeEnrollment = new InternshipEnrollment();
        activeEnrollment.setProgram(otherProgram);

        when(programRepository.findById(1L)).thenReturn(Optional.of(mockProgram));
        when(internProfileRepository.findById(100L)).thenReturn(Optional.of(mockIntern));
        when(enrollmentRepository.existsByProgramIdAndInternId(1L, 100L)).thenReturn(false);
        when(enrollmentRepository.findByInternIdAndStatus(100L, EnrollmentStatus.ACTIVE)).thenReturn(Optional.of(activeEnrollment));

        ConflictException exception = assertThrows(ConflictException.class, () ->
                enrollmentService.enrollIntern(1L, 100L, null, LocalDate.now())
        );

        assertTrue(exception.getMessage().contains("Mỗi thực tập sinh chỉ thuộc 1 chương trình"));
        verify(enrollmentRepository, never()).save(any());
    }
}
