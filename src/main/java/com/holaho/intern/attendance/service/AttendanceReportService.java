package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.dto.AttendanceSummaryResponse;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.LeaveRequestRepository;
import com.holaho.intern.shared.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceReportService {

    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate) {
        Long tenantId = getCurrentTenantId();
        if (fromDate == null) fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null) toDate = LocalDate.now();

        long totalInterns = internProfileRepository.countByTenantId(tenantId);
        List<Attendance> attendances = attendanceRepository.findByDateBetween(fromDate, toDate);

        long present = attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long late = attendances.stream().filter(a -> "LATE".equals(a.getStatus()) || "LATE_AND_EARLY_LEAVE".equals(a.getStatus())).count();
        long earlyLeave = attendances.stream().filter(a -> "EARLY_LEAVE".equals(a.getStatus()) || "LATE_AND_EARLY_LEAVE".equals(a.getStatus())).count();
        long absent = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();

        long totalRecords = attendances.size();
        double attendanceRate = totalRecords == 0 ? 100.0 : ((double) (present + late) / totalRecords) * 100.0;

        return AttendanceSummaryResponse.builder()
                .totalInterns(totalInterns)
                .totalWorkingDays(totalRecords)
                .presentDays(present)
                .lateDays(late)
                .earlyLeaveDays(earlyLeave)
                .absentDays(absent)
                .leaveDays(0)
                .attendanceRate(Math.round(attendanceRate * 10.0) / 10.0)
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
