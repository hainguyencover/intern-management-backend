package com.example.backend.service.impl;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.ReviewWeeklyReportRequest;
import com.example.backend.dto.request.WeeklyReportRequest;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.User;
import com.example.backend.entity.WeeklyReport;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.WeeklyReportMapper;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.WeeklyReportRepository;
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
    private com.example.backend.service.AiService aiService;

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
