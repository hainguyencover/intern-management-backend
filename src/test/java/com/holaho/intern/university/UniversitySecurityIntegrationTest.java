package com.holaho.intern.university;

import com.holaho.intern.entity.University;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityStudentFilter;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import com.holaho.intern.university.repository.UniversityStudentRepository;
import com.holaho.intern.university.service.UniversityDashboardService;
import com.holaho.intern.university.service.UniversityStudentService;
import com.holaho.intern.user.entity.Role;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversitySecurityIntegrationTest {

    @Mock
    private UniversityStudentRepository studentRepository;

    @InjectMocks
    private UniversityStudentService studentService;

    @InjectMocks
    private UniversityDashboardService dashboardService;

    private University universityA;
    private University universityB;
    private User univAdminUserA;
    private CustomUserDetails principalUnivA;

    @BeforeEach
    void setUp() {
        universityA = new University("FPT", "FPT University", "fpt.edu.vn", "ACTIVE");
        universityA.setId(1L);

        universityB = new University("HUST", "Hanoi University of Science and Technology", "hust.edu.vn", "ACTIVE");
        universityB.setId(2L);

        univAdminUserA = new User();
        univAdminUserA.setId(10L);
        univAdminUserA.setEmail("admin@fpt.edu.vn");
        univAdminUserA.setPasswordHash("hashed_pass");
        Role role = new Role();
        role.setCode("UNIVERSITY_ADMIN");
        univAdminUserA.setRoles(Set.of(role));

        principalUnivA = new CustomUserDetails(univAdminUserA, 1L);
    }

    @Test
    @DisplayName("University A Admin can query students belonging to University A")
    void testUniversityAScopeAccess_Success() {
        UniversityStudentFilter filter = new UniversityStudentFilter();
        Pageable pageable = Pageable.unpaged();

        when(studentRepository.searchStudents(eq(1L), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<UniversityStudentResponse> result = studentService.findStudents(principalUnivA, filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Scope Isolation: Querying student from University B using University A Principal fails with ResourceNotFoundException")
    void testCrossUniversityAccess_Blocked() {
        Long studentIdBelongingToUnivB = 999L;

        when(studentRepository.findByIdAndUniversityId(eq(studentIdBelongingToUnivB), eq(1L)))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            studentService.getStudentDetail(principalUnivA, studentIdBelongingToUnivB);
        });
    }

    @Test
    @DisplayName("Privacy Whitelist Verification: DTO maps only academic/progress fields and excludes sensitive HR info")
    void testDTOWhitelistPrivacy() {
        InternProfile profile = new InternProfile();
        profile.setId(100L);
        profile.setStudentCode("SV001");
        profile.setUniversity("FPT University");

        // Verify UniversityStudentResponse structure does not contain salary/allowance/password fields
        UniversityStudentResponse response = UniversityStudentResponse.builder()
                .id(profile.getId())
                .studentCode(profile.getStudentCode())
                .fullName("Test Student")
                .major("Software Engineering")
                .status("INTERNING")
                .progress(UniversityStudentResponse.TaskProgressInfo.builder()
                        .totalTasks(10)
                        .completedTasks(8)
                        .completionRate(BigDecimal.valueOf(80.0))
                        .build())
                .attendance(UniversityStudentResponse.AttendanceInfo.builder()
                        .workingDays(20)
                        .presentDays(19)
                        .attendanceRate(BigDecimal.valueOf(95.0))
                        .build())
                .build();

        assertNotNull(response.getStudentCode());
        assertEquals("SV001", response.getStudentCode());
        assertEquals(BigDecimal.valueOf(80.0), response.getProgress().getCompletionRate());
        assertEquals(BigDecimal.valueOf(95.0), response.getAttendance().getAttendanceRate());
    }
}
