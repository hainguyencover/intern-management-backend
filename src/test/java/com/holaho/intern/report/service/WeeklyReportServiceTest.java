package com.holaho.intern.report.service;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.report.dto.*;
import com.holaho.intern.report.entity.WeeklyReport;
import com.holaho.intern.report.entity.WeeklyReportFeedback;
import com.holaho.intern.report.enums.WeeklyReportStatus;
import com.holaho.intern.report.repository.WeeklyReportFeedbackRepository;
import com.holaho.intern.report.repository.WeeklyReportRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    private WeeklyReportRepository weeklyReportRepository;
    @Mock
    private WeeklyReportFeedbackRepository feedbackRepository;
    @Mock
    private InternProfileRepository internProfileRepository;
    @Mock
    private MentorRepository mentorRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WeeklyReportServiceImpl weeklyReportService;

    private User internUser;
    private User mentorUser;
    private InternProfile intern;
    private Mentor mentor;
    private WeeklyReport draftReport;

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
        intern.setTenantId(1L);

        draftReport = WeeklyReport.builder()
                .intern(intern)
                .mentor(mentor)
                .weekStartDate(LocalDate.of(2026, 8, 10))
                .weekEndDate(LocalDate.of(2026, 8, 16))
                .title("Báo cáo tuần 32")
                .workSummary("Hoàn thành API và Test")
                .status(WeeklyReportStatus.DRAFT)
                .late(false)
                .build();
        draftReport.setId(1001L);
        draftReport.setTenantId(1L);
    }

    @Test
    void createDraft_Success() {
        WeeklyReportCreateRequest req = WeeklyReportCreateRequest.builder()
                .weekStartDate(LocalDate.of(2026, 8, 10))
                .weekEndDate(LocalDate.of(2026, 8, 16))
                .title("Báo cáo tuần 32")
                .workSummary("Nhiệm vụ 1, 2")
                .build();

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));
        when(weeklyReportRepository.existsByTenantIdAndInternIdAndWeekStartDate(anyLong(), eq(100L), eq(req.getWeekStartDate())))
                .thenReturn(false);
        when(weeklyReportRepository.save(any(WeeklyReport.class))).thenAnswer(inv -> {
            WeeklyReport r = inv.getArgument(0);
            r.setId(1001L);
            return r;
        });

        WeeklyReportResponse res = weeklyReportService.createDraft(10L, req);

        assertNotNull(res);
        assertEquals("Báo cáo tuần 32", res.getTitle());
        assertEquals(WeeklyReportStatus.DRAFT, res.getStatus());
        verify(weeklyReportRepository).save(any(WeeklyReport.class));
    }

    @Test
    void createDraft_NoMentor_ThrowsBadRequest() {
        intern.setMentor(null);
        WeeklyReportCreateRequest req = WeeklyReportCreateRequest.builder()
                .weekStartDate(LocalDate.of(2026, 8, 10))
                .weekEndDate(LocalDate.of(2026, 8, 16))
                .title("Báo cáo tuần 32")
                .workSummary("Nhiệm vụ 1")
                .build();

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));

        assertThrows(BadRequestException.class, () -> weeklyReportService.createDraft(10L, req));
    }

    @Test
    void createDraft_DuplicateWeek_ThrowsConflict() {
        WeeklyReportCreateRequest req = WeeklyReportCreateRequest.builder()
                .weekStartDate(LocalDate.of(2026, 8, 10))
                .weekEndDate(LocalDate.of(2026, 8, 16))
                .title("Báo cáo tuần 32")
                .workSummary("Nhiệm vụ 1")
                .build();

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));
        when(weeklyReportRepository.existsByTenantIdAndInternIdAndWeekStartDate(anyLong(), eq(100L), eq(req.getWeekStartDate())))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> weeklyReportService.createDraft(10L, req));
    }

    @Test
    void updateDraft_SubmittedReport_ThrowsBadRequest() {
        draftReport.setStatus(WeeklyReportStatus.SUBMITTED);
        WeeklyReportUpdateRequest req = WeeklyReportUpdateRequest.builder()
                .title("Updated Title")
                .workSummary("Updated summary")
                .build();

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));
        when(weeklyReportRepository.findById(1001L)).thenReturn(Optional.of(draftReport));

        assertThrows(BadRequestException.class, () -> weeklyReportService.updateDraft(1001L, 10L, req));
    }

    @Test
    void submitReport_OnTime_Success() {
        // weekEndDate = 2099-12-31 (future date -> on time)
        draftReport.setWeekEndDate(LocalDate.of(2099, 12, 31));

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));
        when(weeklyReportRepository.findById(1001L)).thenReturn(Optional.of(draftReport));
        when(weeklyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WeeklyReportResponse res = weeklyReportService.submitReport(1001L, 10L);

        assertNotNull(res);
        assertEquals(WeeklyReportStatus.SUBMITTED, res.getStatus());
        assertFalse(res.isLate());
        verify(notificationService).createNotification(eq(20L), eq(NotificationType.WEEKLY_REPORT), anyString(), anyString());
    }

    @Test
    void submitReport_Late_Success() {
        // weekEndDate = 2020-01-01 (past date -> late)
        draftReport.setWeekEndDate(LocalDate.of(2020, 1, 1));

        when(internProfileRepository.findByUser_Id(10L)).thenReturn(Optional.of(intern));
        when(weeklyReportRepository.findById(1001L)).thenReturn(Optional.of(draftReport));
        when(weeklyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WeeklyReportResponse res = weeklyReportService.submitReport(1001L, 10L);

        assertNotNull(res);
        assertEquals(WeeklyReportStatus.LATE, res.getStatus());
        assertTrue(res.isLate());
        verify(notificationService).createNotification(eq(20L), eq(NotificationType.WEEKLY_REPORT), anyString(), anyString());
    }

    @Test
    void getMentorReportById_Forbidden_OtherMentor() {
        when(mentorRepository.findByUser_Id(20L)).thenReturn(Optional.of(mentor));
        when(weeklyReportRepository.findByIdAndTenantIdAndMentorId(eq(1001L), anyLong(), eq(200L)))
                .thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> weeklyReportService.getMentorReportById(1001L, 20L));
    }

    @Test
    void addFeedback_Success() {
        WeeklyReportFeedbackRequest req = WeeklyReportFeedbackRequest.builder()
                .content("Báo cáo làm rất tốt!")
                .build();

        when(mentorRepository.findByUser_Id(20L)).thenReturn(Optional.of(mentor));
        when(weeklyReportRepository.findByIdAndTenantIdAndMentorId(eq(1001L), anyLong(), eq(200L)))
                .thenReturn(Optional.of(draftReport));
        when(feedbackRepository.save(any(WeeklyReportFeedback.class))).thenAnswer(inv -> {
            WeeklyReportFeedback fb = inv.getArgument(0);
            fb.setId(5001L);
            return fb;
        });

        WeeklyReportFeedbackResponse res = weeklyReportService.addFeedback(1001L, 20L, req);

        assertNotNull(res);
        assertEquals("Báo cáo làm rất tốt!", res.getContent());
        verify(feedbackRepository).save(any(WeeklyReportFeedback.class));
        verify(notificationService).createNotification(eq(10L), eq(NotificationType.WEEKLY_REPORT), anyString(), anyString());
    }
}
