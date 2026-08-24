package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.MentorWorkloadProjection;

import com.holaho.intern.mentor.dto.MentorWorkloadResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadSummaryResponse;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentorWorkloadServiceTest {

    @Mock
    private MentorRepository mentorRepository;

    @Mock
    private MentorAssignmentRepository assignmentRepository;

    @InjectMocks
    private MentorWorkloadServiceImpl workloadService;

    @Test
    void testCalculateWorkloadStatus() {
        assertEquals(MentorWorkloadStatus.NO_ASSIGNMENT, workloadService.calculateWorkloadStatus(0, 10));
        assertEquals(MentorWorkloadStatus.UNDERLOAD, workloadService.calculateWorkloadStatus(5, 10)); // 50%
        assertEquals(MentorWorkloadStatus.NORMAL, workloadService.calculateWorkloadStatus(7, 10)); // 70%
        assertEquals(MentorWorkloadStatus.NEAR_CAPACITY, workloadService.calculateWorkloadStatus(9, 10)); // 90%
        assertEquals(MentorWorkloadStatus.NEAR_CAPACITY, workloadService.calculateWorkloadStatus(10, 10)); // 100%
        assertEquals(MentorWorkloadStatus.OVERLOAD, workloadService.calculateWorkloadStatus(11, 10)); // 110%
        assertEquals(MentorWorkloadStatus.OVERLOAD, workloadService.calculateWorkloadStatus(5, 0)); // capacity 0
    }

    @Test
    void testGetWorkloadSummary() {
        Mentor m1 = new Mentor();
        m1.setId(1L);
        m1.setCapacity(10);

        Mentor m2 = new Mentor();
        m2.setId(2L);
        m2.setCapacity(10);

        Mentor m3 = new Mentor();
        m3.setId(3L);
        m3.setCapacity(10);

        when(mentorRepository.findAllByTenantIdWithDetails(anyLong())).thenReturn(List.of(m1, m2, m3));

        MentorWorkloadProjection p1 = createProjection(1L, 5L); // 50% Underload
        MentorWorkloadProjection p2 = createProjection(2L, 12L); // 120% Overload

        when(assignmentRepository.countActiveWorkloadsByTenantId(anyLong())).thenReturn(List.of(p1, p2));

        MentorWorkloadSummaryResponse summary = workloadService.getWorkloadSummary();

        assertNotNull(summary);
        assertEquals(3, summary.getTotalMentors());
        assertEquals(2, summary.getMentorsWithInterns());
        assertEquals(17, summary.getTotalActiveInterns());
        assertEquals(1, summary.getNoAssignmentCount()); // m3 has 0
        assertEquals(1, summary.getUnderloadCount()); // m1 has 5
        assertEquals(1, summary.getOverloadCount()); // m2 has 12
    }

    private MentorWorkloadProjection createProjection(Long mentorId, Long count) {
        return new MentorWorkloadProjection() {
            @Override
            public Long getMentorId() {
                return mentorId;
            }

            @Override
            public Long getActiveInternCount() {
                return count;
            }
        };
    }
}
