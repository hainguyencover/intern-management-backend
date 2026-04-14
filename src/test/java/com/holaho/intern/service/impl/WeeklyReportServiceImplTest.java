package com.holaho.intern.service.impl;

import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.User;
import com.holaho.intern.entity.WeeklyReport;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.mapper.WeeklyReportMapper;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.UserRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceImplTest {

    @Mock
    private WeeklyReportRepository reportRepository;
    @Mock
    private InternProfileRepository internRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WeeklyReportMapper weeklyReportMapper;
    @Mock
    private com.holaho.intern.service.AiService aiService;

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
        assertEquals("REVIEWED", report.getStatus());
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
}

