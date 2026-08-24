package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.enums.AttendanceStatus;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job running daily at 23:59 to automatically mark missing attendance records as ABSENT.
 * Checks whether the intern was on approved leave before marking ABSENT.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceScheduler {

    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Scheduled(cron = "0 59 23 * * ?")
    @Transactional
    public void autoMarkAbsentJob() {
        LocalDate today = LocalDate.now();
        log.info("Running daily auto-absent job for date: {}", today);

        List<InternProfile> activeInterns = internProfileRepository.findAll().stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()) || "INTERNING".equals(i.getStatus()))
                .toList();

        int absentCount = 0;
        int onLeaveCount = 0;

        for (InternProfile intern : activeInterns) {
            boolean hasAttendance = attendanceRepository.existsByInternIdAndDate(intern.getId(), today);
            if (!hasAttendance) {
                Long tenantId = intern.getTenantId() != null ? intern.getTenantId() : 1L;
                boolean isOnLeave = leaveRequestRepository.existsApprovedLeaveOnDate(intern.getId(), today);

                Attendance attendance = Attendance.builder()
                        .intern(intern)
                        .date(today)
                        .status(isOnLeave ? AttendanceStatus.ON_LEAVE.name() : AttendanceStatus.ABSENT.name())
                        .note(isOnLeave ? "Tự động ghi nhận nghỉ phép" : "Tự động ghi nhận vắng mặt (chưa chấm công)")
                        .build();
                attendance.setTenantId(tenantId);

                attendanceRepository.save(attendance);
                if (isOnLeave) onLeaveCount++;
                else absentCount++;
            }
        }

        log.info("Daily auto-absent job completed. Marked ABSENT: {}, ON_LEAVE: {}", absentCount, onLeaveCount);
    }
}
