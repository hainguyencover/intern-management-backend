package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.dto.AttendanceDetailResponse;
import com.holaho.intern.attendance.dto.CheckInRequestDto;
import com.holaho.intern.attendance.dto.CheckOutRequestDto;
import com.holaho.intern.attendance.entity.WorkSchedule;
import com.holaho.intern.attendance.enums.AttendanceStatus;
import com.holaho.intern.attendance.repository.WorkScheduleRepository;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.LeaveRequestRepository;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
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
class AttendanceCoreServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private InternProfileRepository internProfileRepository;

    @Mock
    private WorkScheduleRepository workScheduleRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private AttendanceCoreService attendanceCoreService;

    private InternProfile intern;
    private User user;
    private WorkSchedule defaultSchedule;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Nguyen Van A");

        intern = new InternProfile();
        intern.setId(10L);
        intern.setStatus("ACTIVE");
        intern.setUser(user);
        intern.setStudentCode("SE123456");

        defaultSchedule = WorkSchedule.builder()
                .id(1L)
                .name("Giờ hành chính tiêu chuẩn")
                .startTime(LocalTime.of(8, 30))
                .endTime(LocalTime.of(17, 30))
                .gracePeriodMinutes(15)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("US-A01: Check-in thành công cho thực tập sinh ACTIVE")
    void checkIn_Success() {
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(intern));
        when(leaveRequestRepository.existsApprovedLeaveOnDate(eq(10L), any(LocalDate.class))).thenReturn(false);
        when(attendanceRepository.existsByInternIdAndDate(eq(10L), any(LocalDate.class))).thenReturn(false);
        when(workScheduleRepository.findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Optional.of(defaultSchedule));

        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        CheckInRequestDto req = new CheckInRequestDto();
        req.setMethod("WEB");
        req.setNote("Đúng giờ");

        AttendanceDetailResponse response = attendanceCoreService.checkIn(10L, req);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10L, response.getInternId());
        assertEquals("Nguyen Van A", response.getInternName());
        assertNotNull(response.getCheckIn());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("US-A01 BR-ATT-03: Từ chối check-in nếu ngày hôm đó đã duyệt nghỉ phép")
    void checkIn_Fail_OnApprovedLeave() {
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(intern));
        when(leaveRequestRepository.existsApprovedLeaveOnDate(eq(10L), any(LocalDate.class))).thenReturn(true);

        assertThrows(BadRequestException.class, () -> attendanceCoreService.checkIn(10L, new CheckInRequestDto()));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("US-A01 BR-ATT-02: Từ chối check-in trùng lập trong cùng 1 ngày")
    void checkIn_Fail_DoubleCheckIn() {
        when(internProfileRepository.findById(10L)).thenReturn(Optional.of(intern));
        when(leaveRequestRepository.existsApprovedLeaveOnDate(eq(10L), any(LocalDate.class))).thenReturn(false);
        when(attendanceRepository.existsByInternIdAndDate(eq(10L), any(LocalDate.class))).thenReturn(true);

        assertThrows(ConflictException.class, () -> attendanceCoreService.checkIn(10L, new CheckInRequestDto()));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("US-A02: Check-out thành công và tự động tính số phút làm việc & trễ/về sớm")
    void checkOut_Success() {
        Attendance attendance = Attendance.builder()
                .intern(intern)
                .date(LocalDate.now())
                .checkIn(LocalDateTime.now().minusHours(8))
                .scheduledStartAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30)))
                .scheduledEndAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(17, 30)))
                .lateMinutes(0)
                .status("PRESENT")
                .build();
        attendance.setId(100L);

        when(attendanceRepository.findByInternIdAndDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(attendance);

        CheckOutRequestDto req = new CheckOutRequestDto();
        req.setMethod("WEB");

        AttendanceDetailResponse response = attendanceCoreService.checkOut(10L, req);

        assertNotNull(response);
        assertNotNull(response.getCheckOut());
        assertTrue(response.getWorkedMinutes() >= 475);
        verify(attendanceRepository).save(attendance);
    }

    @Test
    @DisplayName("US-A02 BR-ATT-06: Check-out thất bại khi chưa check-in")
    void checkOut_Fail_NoCheckIn() {
        when(attendanceRepository.findByInternIdAndDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> attendanceCoreService.checkOut(10L, new CheckOutRequestDto()));
    }

    @Test
    @DisplayName("US-A02 BR-ATT-07: Check-out thất bại khi đã check-out rồi")
    void checkOut_Fail_AlreadyCheckedOut() {
        Attendance attendance = Attendance.builder()
                .intern(intern)
                .date(LocalDate.now())
                .checkIn(LocalDateTime.now().minusHours(8))
                .checkOut(LocalDateTime.now().minusHours(1))
                .status("PRESENT")
                .build();

        when(attendanceRepository.findByInternIdAndDate(eq(10L), any(LocalDate.class)))
                .thenReturn(Optional.of(attendance));

        assertThrows(ConflictException.class, () -> attendanceCoreService.checkOut(10L, new CheckOutRequestDto()));
    }
}
