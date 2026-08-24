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
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Core attendance engine enforcing server-side timestamp (`LocalDateTime.now()`),
 * work schedule checks, grace period calculations, and state machine status transitions.
 */
@Service("attendanceCoreService")
@RequiredArgsConstructor
@Slf4j
public class AttendanceCoreService {

    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    public AttendanceDetailResponse checkIn(Long internId, CheckInRequestDto req) {
        Long tenantId = getCurrentTenantId();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now(); // Server-side timestamp source of truth

        // 1. Intern profile check & active internship status
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        if (!"ACTIVE".equals(intern.getStatus()) && !"INTERNING".equals(intern.getStatus())) {
            throw new BadRequestException("Thực tập sinh không trong thời gian thực tập active.");
        }

        // 2. BR-ATT-03: Block check-in if intern has approved leave today
        if (leaveRequestRepository.existsApprovedLeaveOnDate(internId, today)) {
            throw new BadRequestException("Không thể điểm danh vào ngày đã được duyệt nghỉ phép.");
        }

        // 3. BR-ATT-02: Double check-in prevention
        if (attendanceRepository.existsByInternIdAndDate(internId, today)) {
            throw new ConflictException("Bạn đã thực hiện check-in cho ngày hôm nay rồi.");
        }

        // 4. Resolve active WorkSchedule (or default 08:30 -> 17:30, grace period 15m)
        WorkSchedule schedule = workScheduleRepository.findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId)
                .orElse(WorkSchedule.builder()
                        .startTime(LocalTime.of(8, 30))
                        .endTime(LocalTime.of(17, 30))
                        .gracePeriodMinutes(15)
                        .build());

        LocalDateTime scheduledStart = LocalDateTime.of(today, schedule.getStartTime());
        LocalDateTime scheduledEnd = LocalDateTime.of(today, schedule.getEndTime());
        LocalTime graceDeadline = schedule.getStartTime().plusMinutes(schedule.getGracePeriodMinutes());

        // 5. Compute Status & Late Minutes
        String status;
        int lateMinutes = 0;
        if (now.toLocalTime().isAfter(graceDeadline)) {
            status = AttendanceStatus.LATE.name();
            lateMinutes = (int) ChronoUnit.MINUTES.between(scheduledStart, now);
        } else {
            status = AttendanceStatus.PRESENT.name();
        }

        Attendance attendance = Attendance.builder()
                .intern(intern)
                .date(today)
                .checkIn(now)
                .scheduledStartAt(scheduledStart)
                .scheduledEndAt(scheduledEnd)
                .lateMinutes(lateMinutes)
                .checkInMethod(req != null && req.getMethod() != null ? req.getMethod() : "WEB")
                .note(req != null ? req.getNote() : null)
                .status(status)
                .build();
        attendance.setTenantId(tenantId);

        attendance = attendanceRepository.save(attendance);
        log.info("Intern {} checked in at {} (status: {})", internId, now, status);

        return mapToDetailResponse(attendance);
    }

    @Transactional
    public AttendanceDetailResponse checkOut(Long internId, CheckOutRequestDto req) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now(); // Server timestamp source of truth

        Attendance attendance = attendanceRepository.findByInternIdAndDate(internId, today)
                .orElseThrow(() -> new NotFoundException("Chưa có bản ghi check-in ngày hôm nay cho TTS ID: " + internId));

        if (attendance.getCheckOut() != null) {
            throw new ConflictException("Bạn đã thực hiện check-out ngày hôm nay rồi.");
        }

        attendance.setCheckOut(now);
        if (req != null && req.getMethod() != null) {
            attendance.setCheckOutMethod(req.getMethod());
        }

        // Calculate worked minutes & early leave minutes
        long worked = ChronoUnit.MINUTES.between(attendance.getCheckIn(), now);
        attendance.setWorkedMinutes((int) worked);
        attendance.setTotalMinutes((int) worked);

        LocalDateTime scheduledEnd = attendance.getScheduledEndAt() != null
                ? attendance.getScheduledEndAt()
                : LocalDateTime.of(today, LocalTime.of(17, 30));

        int earlyLeaveMins = 0;
        if (now.isBefore(scheduledEnd)) {
            earlyLeaveMins = (int) ChronoUnit.MINUTES.between(now, scheduledEnd);
        }
        attendance.setEarlyLeaveMinutes(earlyLeaveMins);

        // Update overall status
        boolean isLate = attendance.getLateMinutes() != null && attendance.getLateMinutes() > 0;
        boolean isEarly = earlyLeaveMins > 0;

        if (isLate && isEarly) {
            attendance.setStatus(AttendanceStatus.LATE_AND_EARLY_LEAVE.name());
        } else if (isLate) {
            attendance.setStatus(AttendanceStatus.LATE.name());
        } else if (isEarly) {
            attendance.setStatus(AttendanceStatus.EARLY_LEAVE.name());
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT.name());
        }

        attendance = attendanceRepository.save(attendance);
        log.info("Intern {} checked out at {} (worked: {} mins, status: {})", internId, now, worked, attendance.getStatus());

        return mapToDetailResponse(attendance);
    }

    @Transactional(readOnly = true)
    public AttendanceDetailResponse getTodayAttendance(Long internId) {
        Optional<Attendance> attendance = attendanceRepository.findByInternIdAndDate(internId, LocalDate.now());
        return attendance.map(this::mapToDetailResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceDetailResponse> getInternHistory(Long internId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        return attendanceRepository.findWithFilters(tenantId, internId, null, fromDate, toDate, null, pageable)
                .map(this::mapToDetailResponse);
    }

    public AttendanceDetailResponse mapToDetailResponse(Attendance attendance) {
        return AttendanceDetailResponse.builder()
                .id(attendance.getId())
                .internId(attendance.getIntern() != null ? attendance.getIntern().getId() : null)
                .internName(attendance.getIntern() != null && attendance.getIntern().getUser() != null
                        ? attendance.getIntern().getUser().getFullName() : null)
                .studentCode(attendance.getIntern() != null ? attendance.getIntern().getStudentCode() : null)
                .date(attendance.getDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .scheduledStartAt(attendance.getScheduledStartAt())
                .scheduledEndAt(attendance.getScheduledEndAt())
                .totalMinutes(attendance.getTotalMinutes())
                .workedMinutes(attendance.getWorkedMinutes())
                .lateMinutes(attendance.getLateMinutes())
                .earlyLeaveMinutes(attendance.getEarlyLeaveMinutes())
                .checkInMethod(attendance.getCheckInMethod())
                .checkOutMethod(attendance.getCheckOutMethod())
                .status(attendance.getStatus())
                .note(attendance.getNote())
                .createdAt(attendance.getCreatedAt())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
