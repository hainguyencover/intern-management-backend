package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.CreateMentorRequest;
import com.holaho.intern.mentor.dto.MentorStatusUpdateRequest;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.exception.MentorHasActiveAssignmentsException;
import com.holaho.intern.mentor.exception.MentorNotFoundException;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorServiceImplTest {

    @Mock
    private MentorRepository mentorRepository;

    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MentorServiceImpl mentorService;

    private Mentor mockMentor;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(10L);
        user.setEmail("mentor@company.com");
        user.setFullName("Nguyen Van Mentor");

        mockMentor = new Mentor();
        mockMentor.setId(1L);
        mockMentor.setTenantId(1L);
        mockMentor.setUser(user);
        mockMentor.setEmployeeCode("MTR-00001");
        mockMentor.setFullName("Nguyen Van Mentor");
        mockMentor.setStatus(MentorStatus.ACTIVE);
        mockMentor.setCapacity(5);
    }

    @Test
    @DisplayName("Should create mentor successfully when employee code is unique")
    void createMentor_Success() {
        CreateMentorRequest req = CreateMentorRequest.builder()
                .fullName("Nguyen Van Mentor")
                .email("mentor@company.com")
                .employeeCode("MTR-00001")
                .capacity(5)
                .build();

        when(mentorRepository.existsByTenantIdAndEmployeeCode(1L, "MTR-00001")).thenReturn(false);
        when(userRepository.findByEmail("mentor@company.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mentorRepository.save(any())).thenReturn(mockMentor);

        MentorResponse response = mentorService.createMentor(req);

        assertNotNull(response);
        assertEquals("MTR-00001", response.getEmployeeCode());
        verify(mentorRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should throw ConflictException when employee code is duplicate")
    void createMentor_DuplicateCode() {
        CreateMentorRequest req = CreateMentorRequest.builder()
                .fullName("Nguyen Van Mentor")
                .email("mentor@company.com")
                .employeeCode("MTR-00001")
                .build();

        when(mentorRepository.existsByTenantIdAndEmployeeCode(1L, "MTR-00001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> mentorService.createMentor(req));
        verify(mentorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent deactivating mentor when active intern assignments exist")
    void updateStatus_RejectWhenActiveAssignmentsExist() {
        MentorStatusUpdateRequest req = MentorStatusUpdateRequest.builder()
                .status(MentorStatus.INACTIVE)
                .forceDeactivate(false)
                .build();

        when(mentorRepository.findByTenantIdAndId(1L, 1L)).thenReturn(Optional.of(mockMentor));
        when(mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(1L, 1L, MentorAssignmentStatus.ACTIVE))
                .thenReturn(3L);

        assertThrows(MentorHasActiveAssignmentsException.class, () -> mentorService.updateMentorStatus(1L, req));
    }
}
