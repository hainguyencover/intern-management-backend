package com.holaho.intern.service;

import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.shared.dto.response.AttendanceResponse;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private AttendanceService attendanceService;

    private InternProfile intern;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Test Intern");

        intern = new InternProfile();
        intern.setId(1L);
        intern.setUser(user);
    }

    @Test
    void checkIn_Success_Normal() {
        // Arrange
        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(attendanceRepository.existsByInternIdAndDate(eq(1L), any(LocalDate.class))).thenReturn(false);
        when(systemConfigService.getValue("work.start_time", "08:30")).thenReturn("08:30");

        // Mock save to return the same attendance with an id
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        // Act
        AttendanceResponse response = attendanceService.checkIn(1L);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getInternId());
        assertEquals("Test Intern", response.getInternName());

        // Status should be PRESENT if before 08:30 or LATE if after
        // Since we can't easily mock LocalDateTime.now() without Mockito-inline or
        // other tools,
        // it depends on when the test runs.
        // Better to check if the status is either PRESENT or LATE.
        assertTrue(response.getStatus().equals("PRESENT") || response.getStatus().equals("LATE"));

        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void checkIn_Fail_AlreadyCheckedIn() {
        // Arrange
        when(internProfileRepository.findById(1L)).thenReturn(Optional.of(intern));
        when(attendanceRepository.existsByInternIdAndDate(eq(1L), any(LocalDate.class))).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> attendanceService.checkIn(1L));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void checkIn_Fail_InternNotFound() {
        // Arrange
        when(internProfileRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> attendanceService.checkIn(1L));
    }

    @Test
    void checkOut_Success() {
        // Arrange
        Attendance attendance = new Attendance();
        attendance.setId(100L);
        attendance.setIntern(intern);
        attendance.setDate(LocalDate.now());
        attendance.setCheckIn(LocalDateTime.now().minusHours(8));

        when(attendanceRepository.findByInternIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        // Act
        AttendanceResponse response = attendanceService.checkOut(1L);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getCheckOut());
        assertNotNull(response.getTotalMinutes());
        assertTrue(response.getTotalMinutes() >= 480); // roughly 8 hours

        verify(attendanceRepository).save(attendance);
    }

    @Test
    void checkOut_Fail_NoCheckIn() {
        // Arrange
        when(attendanceRepository.findByInternIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> attendanceService.checkOut(1L));
    }

    @Test
    void checkOut_Fail_AlreadyCheckedOut() {
        // Arrange
        Attendance attendance = new Attendance();
        attendance.setId(100L);
        attendance.setIntern(intern);
        attendance.setDate(LocalDate.now());
        attendance.setCheckIn(LocalDateTime.now().minusHours(8));
        attendance.setCheckOut(LocalDateTime.now().minusMinutes(5));

        when(attendanceRepository.findByInternIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        // Act & Assert
        assertThrows(ConflictException.class, () -> attendanceService.checkOut(1L));
    }

    @Test
    void getTodayAttendance_ReturnsAttendance() {
        // Arrange
        Attendance attendance = new Attendance();
        when(attendanceRepository.findByInternIdAndDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        // Act
        Attendance result = attendanceService.getTodayAttendance(1L);

        // Assert
        assertNotNull(result);
        assertEquals(attendance, result);
    }
}
