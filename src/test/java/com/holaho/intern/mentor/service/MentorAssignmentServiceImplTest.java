package com.holaho.intern.mentor.service;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.InternshipEnrollmentRepository;
import com.holaho.intern.shared.dto.response.AssignMentorResponse;
import com.holaho.intern.shared.dto.response.MentorCapacityResponse;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorAssignmentServiceImplTest {

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private MentorRepository mentorRepository;

    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;

    @Mock
    private InternshipEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private MentorAssignmentServiceImpl mentorAssignmentService;

    private InternProfile mockIntern;
    private Mentor mockMentor;
    private User mockInternUser;
    private User mockMentorUser;

    @BeforeEach
    void setUp() {
        mockInternUser = new User();
        mockInternUser.setId(100L);
        mockInternUser.setFullName("Intern Test");
        mockInternUser.setEmail("intern@test.com");
        mockInternUser.setStatus(UserStatus.ACTIVE);

        mockIntern = new InternProfile();
        mockIntern.setId(10L);
        mockIntern.setUser(mockInternUser);

        mockMentorUser = new User();
        mockMentorUser.setId(200L);
        mockMentorUser.setFullName("Mentor Test");
        mockMentorUser.setEmail("mentor@test.com");
        mockMentorUser.setStatus(UserStatus.ACTIVE);

        mockMentor = new Mentor();
        mockMentor.setId(20L);
        mockMentor.setUser(mockMentorUser);
    }

    @Test
    @DisplayName("Should assign mentor successfully when capacity < 5")
    void shouldAssignMentorSuccessfully() {
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(mockIntern));
        when(mentorRepository.findByIdWithLock(20L)).thenReturn(Optional.of(mockMentor));
        when(mentorAssignmentRepository.countByMentorIdAndStatus(20L, MentorAssignmentStatus.ACTIVE)).thenReturn(3L);
        when(mentorAssignmentRepository.findByInternIdAndStatus(10L, MentorAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());
        when(internProfileRepository.save(any(InternProfile.class))).thenReturn(mockIntern);

        AssignMentorResponse response = mentorAssignmentService.assignMentorToIntern(10L, 20L, "Initial assignment");

        assertNotNull(response);
        assertEquals(10L, response.getInternId());
        assertEquals(20L, response.getMentorId());

        verify(mentorAssignmentRepository, times(1)).save(any(MentorAssignment.class));
        verify(internProfileRepository, times(1)).save(mockIntern);
    }

    @Test
    @DisplayName("BR-01: Should throw 409 CONFLICT when mentor has reached maximum capacity of 5")
    void shouldRejectAssignmentWhenMentorCapacityExceeded() {
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(mockIntern));
        when(mentorRepository.findByIdWithLock(20L)).thenReturn(Optional.of(mockMentor));
        when(mentorAssignmentRepository.countByMentorIdAndStatus(20L, MentorAssignmentStatus.ACTIVE)).thenReturn(5L);

        ApiException exception = assertThrows(ApiException.class, () ->
                mentorAssignmentService.assignMentorToIntern(10L, 20L, "Capacity overflow test")
        );

        assertEquals(HttpStatus.CONFLICT, exception.status);
        assertTrue(exception.getMessage().contains("MENTOR_CAPACITY_EXCEEDED"));
        verify(mentorAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("US-046: Should close previous assignment when reassigning new mentor")
    void shouldClosePreviousAssignmentWhenChangingMentor() {
        Mentor oldMentor = new Mentor();
        oldMentor.setId(30L);
        mockIntern.setMentor(oldMentor);

        MentorAssignment previousAssignment = new MentorAssignment();
        previousAssignment.setId(500L);
        previousAssignment.setMentor(oldMentor);
        previousAssignment.setIntern(mockIntern);
        previousAssignment.setStatus(MentorAssignmentStatus.ACTIVE);

        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(mockIntern));
        when(mentorRepository.findByIdWithLock(20L)).thenReturn(Optional.of(mockMentor));
        when(mentorAssignmentRepository.countByMentorIdAndStatus(20L, MentorAssignmentStatus.ACTIVE)).thenReturn(2L);
        when(mentorAssignmentRepository.findByInternIdAndStatus(10L, MentorAssignmentStatus.ACTIVE)).thenReturn(Optional.of(previousAssignment));
        when(internProfileRepository.save(any(InternProfile.class))).thenReturn(mockIntern);

        mentorAssignmentService.assignMentorToIntern(10L, 20L, "Reassign mentor");

        assertEquals(MentorAssignmentStatus.ENDED, previousAssignment.getStatus());
        assertNotNull(previousAssignment.getEndedAt());
        verify(mentorAssignmentRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Should return mentor capacity details accurately")
    void shouldReturnMentorCapacityDetails() {
        when(mentorRepository.findByTenantIdAndId(1L, 20L)).thenReturn(Optional.of(mockMentor));
        when(mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(1L, 20L, MentorAssignmentStatus.ACTIVE)).thenReturn(4L);

        MentorCapacityResponse capacity = mentorAssignmentService.getMentorCapacity(20L);

        assertNotNull(capacity);
        assertEquals(20L, capacity.getMentorId());
        assertEquals(4L, capacity.getActiveInternCount());
        assertEquals(5, capacity.getMaxCapacity());
        assertFalse(capacity.isFull());
        assertEquals(80.0, capacity.getCapacityPercentage());
    }

    @Test
    @DisplayName("US Gỡ Mentor: Should unassign active mentor successfully and set status UNASSIGNED")
    void shouldUnassignMentorSuccessfully() {
        MentorAssignment activeAssignment = new MentorAssignment();
        activeAssignment.setId(101L);
        activeAssignment.setMentor(mockMentor);
        activeAssignment.setIntern(mockIntern);
        activeAssignment.setStatus(MentorAssignmentStatus.ACTIVE);
        mockIntern.setMentor(mockMentor);

        when(mentorAssignmentRepository.findByTenantIdAndId(1L, 101L)).thenReturn(Optional.of(activeAssignment));
        when(mentorAssignmentRepository.save(any(MentorAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        com.holaho.intern.mentor.dto.UnassignMentorRequest request =
                new com.holaho.intern.mentor.dto.UnassignMentorRequest("Mentor changed department");

        com.holaho.intern.shared.dto.response.MentorAssignmentResponse response =
                mentorAssignmentService.unassignMentor(101L, request);

        assertNotNull(response);
        assertEquals(MentorAssignmentStatus.UNASSIGNED, response.getStatus());
        assertEquals("Mentor changed department", response.getUnassignReason());
        assertNull(mockIntern.getMentor());
        verify(mentorAssignmentRepository, times(1)).save(activeAssignment);
        verify(internProfileRepository, times(1)).save(mockIntern);
    }

    @Test
    @DisplayName("US Gỡ Mentor: Should throw 409 CONFLICT when assignment is not active")
    void shouldThrowConflictWhenUnassigningInactiveAssignment() {
        MentorAssignment inactiveAssignment = new MentorAssignment();
        inactiveAssignment.setId(102L);
        inactiveAssignment.setMentor(mockMentor);
        inactiveAssignment.setIntern(mockIntern);
        inactiveAssignment.setStatus(MentorAssignmentStatus.UNASSIGNED);

        when(mentorAssignmentRepository.findByTenantIdAndId(1L, 102L)).thenReturn(Optional.of(inactiveAssignment));

        com.holaho.intern.mentor.dto.UnassignMentorRequest request =
                new com.holaho.intern.mentor.dto.UnassignMentorRequest("Test reason");

        ApiException exception = assertThrows(ApiException.class, () ->
                mentorAssignmentService.unassignMentor(102L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.status);
        assertTrue(exception.getMessage().contains("Mentor assignment is not active"));
        verify(mentorAssignmentRepository, never()).save(any());
    }
}
