package com.holaho.intern.service;

import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.shared.dto.request.EvaluationRequest;
import com.holaho.intern.shared.dto.response.EvaluationResponse;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private InternProfileRepository internRepository;
    @Mock
    private MentorRepository mentorRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EvaluationService evaluationService;

    private User internUser;
    private User mentorUser;
    private InternProfile intern;
    private Mentor mentor;

    @BeforeEach
    void setUp() {
        internUser = new User();
        internUser.setId(10L);
        internUser.setFullName("Intern Nguyen");
        internUser.setEmail("intern@test.com");

        mentorUser = new User();
        mentorUser.setId(20L);
        mentorUser.setFullName("Mentor Tran");
        mentorUser.setEmail("mentor@test.com");

        mentor = new Mentor();
        mentor.setId(200L);
        mentor.setUser(mentorUser);
        mentor.setTenantId(1L);

        intern = new InternProfile();
        intern.setId(100L);
        intern.setUser(internUser);
        intern.setMentor(mentor);
        intern.setStatus("ACTIVE");
        intern.setTenantId(1L);
    }

    @Test
    void createEvaluation_CalculatesWeightedScore_Success() {
        EvaluationRequest req = EvaluationRequest.builder()
                .internId(100L)
                .period("Tháng 02/2026")
                .technicalScore(9.0)
                .workQualityScore(8.0)
                .attitudeScore(9.0)
                .softSkillScore(8.0)
                .comment("Thái độ làm việc rất tốt, hoàn thành đúng hạn.")
                .build();

        when(internRepository.findById(100L)).thenReturn(Optional.of(intern));
        when(mentorRepository.findByUser_Id(20L)).thenReturn(Optional.of(mentor));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(1001L);
            return e;
        });

        EvaluationResponse res = evaluationService.create(req, 20L);

        assertNotNull(res);
        // weighted = 0.35*9.0 + 0.30*8.0 + 0.20*9.0 + 0.15*8.0 = 3.15 + 2.40 + 1.80 + 1.20 = 8.55
        assertEquals(8.55, res.getWeightedScore());
        assertEquals("PASS", res.getResultStatus());
        assertEquals("B", res.getGrade());
        verify(evaluationRepository).save(any(Evaluation.class));
        verify(notificationService).createNotification(eq(10L), eq(NotificationType.SYSTEM), anyString(), anyString());
    }

    @Test
    void createFinalEvaluation_PassBR04_AutoCompletesIntern() {
        EvaluationRequest req = EvaluationRequest.builder()
                .internId(100L)
                .period("FINAL")
                .technicalScore(8.0)
                .workQualityScore(8.0)
                .attitudeScore(8.0)
                .softSkillScore(8.0)
                .comment("Hoàn thành chương trình thực tập thành công")
                .build();

        when(internRepository.findById(100L)).thenReturn(Optional.of(intern));
        when(mentorRepository.findByUser_Id(20L)).thenReturn(Optional.of(mentor));
        when(attendanceRepository.findByInternId(100L)).thenReturn(List.of());
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(1002L);
            return e;
        });

        EvaluationResponse res = evaluationService.create(req, 20L);

        assertNotNull(res);
        assertEquals("COMPLETED", intern.getStatus());
        verify(internRepository).save(intern);
    }
}
