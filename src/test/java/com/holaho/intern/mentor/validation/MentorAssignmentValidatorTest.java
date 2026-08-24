package com.holaho.intern.mentor.validation;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.exception.InvalidMentorAssignmentException;
import com.holaho.intern.mentor.exception.MentorCapacityExceededException;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import com.holaho.intern.shared.enums.MentorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorAssignmentValidatorTest {

    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;

    @InjectMocks
    private MentorAssignmentValidator validator;

    private Mentor mentor;
    private InternProfile intern;

    @BeforeEach
    void setUp() {
        mentor = new Mentor();
        mentor.setId(100L);
        mentor.setTenantId(1L);
        mentor.setFullName("Mentor A");
        mentor.setStatus(MentorStatus.ACTIVE);
        mentor.setCapacity(5);

        intern = new InternProfile();
        intern.setId(200L);
        intern.setTenantId(1L);
    }

    @Test
    @DisplayName("BR-MEN-01: Should pass when mentor and intern belong to same tenant")
    void validateTenantMatch_Success() {
        assertDoesNotThrow(() -> validator.validateTenantMatch(mentor, intern));
    }

    @Test
    @DisplayName("BR-MEN-01: Should throw exception when mentor and intern belong to different tenants")
    void validateTenantMatch_CrossTenantFailure() {
        mentor.setTenantId(1L);
        intern.setTenantId(2L);

        InvalidMentorAssignmentException ex = assertThrows(
                InvalidMentorAssignmentException.class,
                () -> validator.validateTenantMatch(mentor, intern)
        );

        assertTrue(ex.getMessage().contains("Cross-tenant assignment rejected"));
    }

    @Test
    @DisplayName("BR-MEN-03: Should pass when mentor status is ACTIVE")
    void validateMentorActive_Success() {
        assertDoesNotThrow(() -> validator.validateMentorActive(mentor));
    }

    @Test
    @DisplayName("BR-MEN-03: Should throw exception when mentor is INACTIVE")
    void validateMentorActive_InactiveFailure() {
        mentor.setStatus(MentorStatus.INACTIVE);

        InvalidMentorAssignmentException ex = assertThrows(
                InvalidMentorAssignmentException.class,
                () -> validator.validateMentorActive(mentor)
        );

        assertTrue(ex.getMessage().contains("Cannot assign mentor"));
    }

    @Test
    @DisplayName("BR-MEN-04: Should throw exception when active count >= capacity")
    void validateMentorCapacity_Exceeded() {
        when(mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(1L, 100L, MentorAssignmentStatus.ACTIVE))
                .thenReturn(5L);

        assertThrows(
                MentorCapacityExceededException.class,
                () -> validator.validateMentorCapacity(mentor, 1L)
        );
    }

    @Test
    @DisplayName("BR-MEN-05: Should throw exception when primary assignment overlaps existing date range")
    void validatePrimaryAssignmentOverlap_Overlap() {
        MentorAssignment existing = new MentorAssignment();
        existing.setId(50L);
        existing.setMentor(mentor);
        existing.setStartDate(LocalDate.of(2026, 9, 1));
        existing.setEndDate(LocalDate.of(2026, 12, 31));

        when(mentorAssignmentRepository.findActivePrimaryAssignmentsForIntern(1L, 200L, MentorAssignmentStatus.ACTIVE, MentorAssignmentType.PRIMARY))
                .thenReturn(List.of(existing));

        assertThrows(
                InvalidMentorAssignmentException.class,
                () -> validator.validatePrimaryAssignmentOverlap(1L, 200L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30), null)
        );
    }
}
