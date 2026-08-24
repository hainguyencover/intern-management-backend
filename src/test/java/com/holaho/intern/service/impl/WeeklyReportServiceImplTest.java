package com.holaho.intern.service.impl;

import com.holaho.intern.entity.WeeklyReport;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.service.WeeklyReportServiceImpl;
import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.shared.dto.response.EvaluationResponse;
import com.holaho.intern.shared.dto.response.FinalReportDto;
import com.holaho.intern.shared.dto.response.FinalReportSummaryDto;
import com.holaho.intern.shared.enums.WeeklyReportStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.mapper.EvaluationMapper;
import com.holaho.intern.shared.mapper.FinalReportMapper;
import com.holaho.intern.shared.mapper.WeeklyReportMapper;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceImplTest {

    @Mock
    private WeeklyReportRepository reportRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private InternProfileRepository internRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyReportMapper weeklyReportMapper;
    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private FinalReportMapper finalReportMapper;
    @Mock
    private com.holaho.intern.service.AiService aiService;
    @Mock
    private MentorRepository mentorRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private WeeklyReportServiceImpl weeklyReportService;

    @Test
    void internSubmit_Success() {
        // Arrange
        Long internId = 1L;
        WeeklyReportRequest req = new WeeklyReportRequest();
        req.setWeekNumber(1);
        req.setCompletedWork("Task A");

        InternProfile intern = new InternProfile();
        intern.setId(internId);

        WeeklyReport savedReport = new WeeklyReport();
        savedReport.setId(100L);

        when(internRepository.findById(internId)).thenReturn(Optional.of(intern));
        when(reportRepository.save(any(WeeklyReport.class))).thenReturn(savedReport);
        when(weeklyReportMapper.toDto(any())).thenReturn(WeeklyReportDto.builder().build());

        // Act
        WeeklyReportDto result = weeklyReportService.internSubmit(internId, req);

        // Assert
        assertNotNull(result);
        verify(reportRepository).save(any(WeeklyReport.class));
    }

    @Test
    void mentorReview_Success() {
        // Arrange
        Long mentorUserId = 2L;
        Long reportId = 100L;
        ReviewWeeklyReportRequest req = new ReviewWeeklyReportRequest("Good job", 5);

        WeeklyReport report = new WeeklyReport();
        report.setId(reportId);

        User mentor = new User();
        mentor.setId(mentorUserId);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(userRepository.findById(mentorUserId)).thenReturn(Optional.of(mentor));
        when(reportRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(weeklyReportMapper.toDto(any())).thenReturn(WeeklyReportDto.builder().build());

        // Act
        WeeklyReportDto result = weeklyReportService.mentorReview(mentorUserId, reportId, req);

        // Assert
        assertNotNull(result);
        assertEquals(WeeklyReportStatus.REVIEWED, report.getStatus());
        assertEquals("Good job", report.getMentorFeedback());
        assertEquals(5, report.getRating());
    }

    @Test
    void internSubmit_InternNotFound_ThrowsException() {
        // Arrange
        when(internRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> weeklyReportService.internSubmit(1L, new WeeklyReportRequest()));
    }

    @Test
    void getFinalReport_Success() {
        // Arrange
        Long internId = 1L;
        InternProfile intern = new InternProfile();
        intern.setId(internId);

        List<Evaluation> evaluations = List.of(new Evaluation());
        evaluations.get(0).setScore(8);

        List<WeeklyReport> reports = List.of(new WeeklyReport());

        when(internRepository.findById(internId)).thenReturn(Optional.of(intern));
        when(evaluationRepository.findByInternId(internId)).thenReturn(evaluations);
        when(reportRepository.findByIntern_IdOrderByWeekNumberDesc(internId)).thenReturn(reports);

        when(evaluationMapper.toResponse(any())).thenReturn(EvaluationResponse.builder().build());
        when(weeklyReportMapper.toDto(any())).thenReturn(WeeklyReportDto.builder().build());
        when(finalReportMapper.toDto(any(), any(), any(), any(), any(), anyDouble(), anyString(), anyInt()))
                .thenReturn(FinalReportDto.builder().build());

        // Act
        FinalReportDto result = weeklyReportService.getFinalReport(internId);

        // Assert
        assertNotNull(result);
        verify(finalReportMapper).toDto(eq(intern), anyString(), anyString(), anyList(), anyList(), eq(8.0), eq("Giỏi"),
                eq(1));
    }

    @Test
    void getReportsSummary_Success() {
        // Arrange
        InternProfile intern = new InternProfile();
        intern.setId(1L);
        when(internRepository.findAll()).thenReturn(List.of(intern));
        when(evaluationRepository.findAverageScoresGroupedByIntern()).thenReturn(Collections.emptyList());
        when(groupMemberRepository.findFirstByIntern_IdAndLeftAtIsNull(1L)).thenReturn(Optional.empty());
        List<Object[]> reportCounts = new java.util.ArrayList<>();
        reportCounts.add(new Object[]{1L, 5L});
        when(reportRepository.countReportsGroupedByIntern()).thenReturn(reportCounts);
        when(finalReportMapper.toSummaryDto(any(), any(), anyDouble(), anyString(), anyInt()))
                .thenReturn(FinalReportSummaryDto.builder().build());

        // Act
        List<FinalReportSummaryDto> result = weeklyReportService.getReportsSummary();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(finalReportMapper).toSummaryDto(eq(intern), anyString(), eq(0.0), eq("Chưa đánh giá"), eq(5));
    }

    @Test
    void updateStatus_Success() {
        // Arrange
        Long reportId = 100L;
        WeeklyReport report = new WeeklyReport();
        report.setId(reportId);
        report.setStatus(WeeklyReportStatus.SUBMITTED);

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(reportRepository.save(any())).thenReturn(report);
        when(weeklyReportMapper.toDto(any())).thenReturn(WeeklyReportDto.builder().build());

        // Act
        WeeklyReportDto result = weeklyReportService.updateStatus(reportId, "REVIEWED");

        // Assert
        assertNotNull(result);
        assertEquals(WeeklyReportStatus.REVIEWED, report.getStatus());
        assertNotNull(report.getReviewedAt());
    }
}
