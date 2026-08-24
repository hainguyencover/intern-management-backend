package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.dto.AttendanceCorrectionDto;
import com.holaho.intern.attendance.dto.CreateCorrectionRequestDto;
import com.holaho.intern.attendance.dto.ReviewCorrectionRequestDto;
import com.holaho.intern.attendance.entity.AttendanceCorrection;
import com.holaho.intern.attendance.repository.AttendanceCorrectionRepository;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class AttendanceCorrectionServiceTest {

    @Mock
    private AttendanceCorrectionRepository correctionRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @InjectMocks
    private AttendanceCorrectionService correctionService;

    private InternProfile intern;
    private Attendance attendance;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Tran Van B");

        intern = new InternProfile();
        intern.setId(5L);
        intern.setUser(user);

        attendance = Attendance.builder()
                .intern(intern)
                .date(LocalDate.now().minusDays(1))
                .checkIn(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(8, 30)))
                .scheduledStartAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(8, 30)))
                .scheduledEndAt(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(17, 30)))
                .status("INCOMPLETE")
                .build();
        attendance.setId(50L);
    }

    @Test
    @DisplayName("US-A04: Tạo yêu cầu sửa công thành công")
    void createCorrectionRequest_Success() {
        when(attendanceRepository.findById(50L)).thenReturn(Optional.of(attendance));
        when(correctionRepository.existsByAttendanceIdAndStatus(50L, "PENDING")).thenReturn(false);
        when(correctionRepository.save(any(AttendanceCorrection.class))).thenAnswer(invocation -> {
            AttendanceCorrection c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CreateCorrectionRequestDto dto = new CreateCorrectionRequestDto();
        dto.setAttendanceId(50L);
        dto.setRequestedCheckIn(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(8, 30)));
        dto.setRequestedCheckOut(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(17, 30)));
        dto.setReason("Quên check-out cuối ngày");

        AttendanceCorrectionDto result = correctionService.createCorrectionRequest(5L, dto);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("Quên check-out cuối ngày", result.getReason());
        verify(correctionRepository).save(any(AttendanceCorrection.class));
    }

    @Test
    @DisplayName("US-A04: Từ chối tạo yêu cầu khi đã có request PENDING cho attendance record đó")
    void createCorrectionRequest_Fail_AlreadyPending() {
        when(attendanceRepository.findById(50L)).thenReturn(Optional.of(attendance));
        when(correctionRepository.existsByAttendanceIdAndStatus(50L, "PENDING")).thenReturn(true);

        CreateCorrectionRequestDto dto = new CreateCorrectionRequestDto();
        dto.setAttendanceId(50L);
        dto.setReason("Quên check out");

        assertThrows(BadRequestException.class, () -> correctionService.createCorrectionRequest(5L, dto));
    }

    @Test
    @DisplayName("US-A07: HR duyệt yêu cầu sửa công và cập nhật lại bản ghi attendance chính")
    void approveCorrectionRequest_Success() {
        AttendanceCorrection correction = AttendanceCorrection.builder()
                .attendance(attendance)
                .intern(intern)
                .requestedCheckIn(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(8, 30)))
                .requestedCheckOut(LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(17, 30)))
                .reason("Quên check out")
                .status("PENDING")
                .build();
        correction.setId(1L);

        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        when(correctionRepository.save(any(AttendanceCorrection.class))).thenReturn(correction);

        ReviewCorrectionRequestDto req = new ReviewCorrectionRequestDto();
        req.setReviewComment("Đã kiểm tra camera, duyệt");

        AttendanceCorrectionDto result = correctionService.approveCorrectionRequest(1L, 99L, req);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("PRESENT", attendance.getStatus());
        assertNotNull(attendance.getCheckOut());
        verify(attendanceRepository).save(attendance);
    }
}
