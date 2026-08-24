package com.holaho.intern.university.service;

import com.holaho.intern.shared.exception.ResourceNotFoundException;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityStudentFilter;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import com.holaho.intern.university.repository.UniversityStudentRepository;
import com.holaho.intern.university.repository.projection.UniversityStudentProjection;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversityStudentServiceTest {

    @Mock
    private UniversityStudentRepository studentRepository;

    @InjectMocks
    private UniversityStudentService studentService;

    private CustomUserDetails mockPrincipal;
    private UniversityStudentProjection mockProjection;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(10L);
        user.setEmail("uni_user@university.edu");
        user.setPasswordHash("hashed_password");
        user.setStatus(User.UserStatus.ACTIVE);

        mockPrincipal = new CustomUserDetails(user, Collections.emptyList(), 1L);

        mockProjection = new UniversityStudentProjection() {
            @Override public Long getId() { return 100L; }
            @Override public String getStudentCode() { return "SV001"; }
            @Override public String getFullName() { return "Nguyen Van A"; }
            @Override public String getMajor() { return "SE"; }
            @Override public String getStatus() { return "INTERNING"; }
            @Override public String getMentorName() { return "Mentor X"; }
            @Override public String getProgramName() { return "Program Summer 2026"; }
            @Override public Long getTotalTasks() { return 10L; }
            @Override public Long getCompletedTasks() { return 8L; }
            @Override public Long getOverdueTasks() { return 1L; }
            @Override public Long getWorkingDays() { return 20L; }
            @Override public Long getPresentDays() { return 19L; }
            @Override public Long getLeaveDays() { return 1L; }
            @Override public BigDecimal getAttendanceRate() { return new BigDecimal("95.00"); }
            @Override public BigDecimal getOverallScore() { return new BigDecimal("8.5"); }
            @Override public String getEvaluationStatus() { return "COMPLETED"; }
        };
    }

    @Test
    @DisplayName("Should return paged student responses for valid university principal")
    void givenValidFilter_whenFindStudents_thenReturnPagedStudents() {
        UniversityStudentFilter filter = new UniversityStudentFilter();
        Pageable pageable = PageRequest.of(0, 10);

        Page<UniversityStudentProjection> page = new PageImpl<>(List.of(mockProjection));
        when(studentRepository.searchStudents(eq(1L), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

        Page<UniversityStudentResponse> result = studentService.findStudents(mockPrincipal, filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        UniversityStudentResponse student = result.getContent().get(0);
        assertEquals("SV001", student.getStudentCode());
        assertEquals("Nguyen Van A", student.getFullName());
        assertEquals(new BigDecimal("80.00"), student.getProgress().getCompletionRate());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when student detail does not belong to university")
    void givenInvalidStudentId_whenGetStudentDetail_thenThrowException() {
        when(studentRepository.findByIdAndUniversityId(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                studentService.getStudentDetail(mockPrincipal, 999L)
        );
    }
}
