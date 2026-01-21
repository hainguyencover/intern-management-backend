package com.example.backend.service.impl;

import com.example.backend.dto.response.AttendanceDailyItem;
import com.example.backend.dto.response.AttendanceSummaryItem;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.LeaveRequest;
import com.example.backend.enums.LeaveStatus;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.LeaveRequestRepository;
import com.example.backend.service.AttendanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final InternProfileRepository internProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryItem> summary(LocalDate from, LocalDate to) {
        // 1) list interns
        List<InternProfile> interns = internProfileRepository.findAll();

        // 2) load attendance in range
        List<Attendance> atts = attendanceRepository.findAllInRange(from, to);
        Map<Long, Set<LocalDate>> presentDatesByIntern = new HashMap<>();
        for (Attendance a : atts) {
            Long internId = a.getIntern().getId();
            presentDatesByIntern.computeIfAbsent(internId, k -> new HashSet<>()).add(a.getDate());
        }

        // 3) load leave APPROVED overlapping range
        List<LeaveRequest> leaves = leaveRequestRepository.findByStatusOverlappingRange(LeaveStatus.APPROVED, from, to);
        Map<Long, List<LeaveRequest>> leavesByIntern = leaves.stream()
                .collect(Collectors.groupingBy(lr -> lr.getIntern().getId()));

        // 4) iterate working days
        List<LocalDate> workingDays = workingDays(from, to);

        List<AttendanceSummaryItem> res = new ArrayList<>();
        for (InternProfile intern : interns) {
            Long internId = intern.getId();
            Set<LocalDate> present = presentDatesByIntern.getOrDefault(internId, Collections.emptySet());
            List<LeaveRequest> lrs = leavesByIntern.getOrDefault(internId, Collections.emptyList());

            int presentDays = 0, leaveDays = 0, absentDays = 0;

            for (LocalDate d : workingDays) {
                if (present.contains(d)) {
                    presentDays++;
                } else if (isOnApprovedLeave(d, lrs)) {
                    leaveDays++;
                } else {
                    absentDays++;
                }
            }

            res.add(AttendanceSummaryItem.builder()
                    .internId(internId)
                    .presentDays(presentDays)
                    .leaveDays(leaveDays)
                    .absentDays(absentDays)
                    .build());
        }
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDailyItem> daily(Long internId, LocalDate from, LocalDate to) {
        List<Attendance> atts = attendanceRepository.findByInternInRange(internId, from, to);
        Map<LocalDate, Attendance> attByDate = atts.stream()
                .collect(Collectors.toMap(Attendance::getDate, x -> x, (a, b) -> a));

        List<LeaveRequest> leaves = leaveRequestRepository
                .findByInternAndStatusOverlappingRange(internId, LeaveStatus.APPROVED, from, to);

        List<AttendanceDailyItem> items = new ArrayList<>();
        for (LocalDate d : workingDays(from, to)) {
            Attendance a = attByDate.get(d);
            if (a != null) {
                items.add(AttendanceDailyItem.builder()
                        .date(d)
                        .status("PRESENT")
                        .checkIn(a.getCheckIn())
                        .checkOut(a.getCheckOut())
                        .build());
            } else {
                LeaveRequest lr = findLeaveCoveringDate(d, leaves);
                if (lr != null) {
                    items.add(AttendanceDailyItem.builder()
                            .date(d)
                            .status("LEAVE")
                            .leaveType(lr.getType().name())
                            .leaveReason(lr.getReason())
                            .build());
                } else {
                    items.add(AttendanceDailyItem.builder()
                            .date(d)
                            .status("ABSENT")
                            .build());
                }
            }
        }
        return items;
    }

    private static boolean isOnApprovedLeave(LocalDate d, List<LeaveRequest> lrs) {
        return findLeaveCoveringDate(d, lrs) != null;
    }

    private static LeaveRequest findLeaveCoveringDate(LocalDate d, List<LeaveRequest> lrs) {
        for (LeaveRequest lr : lrs) {
            if (!d.isBefore(lr.getFromDate()) && !d.isAfter(lr.getToDate())) return lr;
        }
        return null;
    }

    // mặc định tính ngày làm việc T2-T6 (bỏ T7/CN)
    private static List<LocalDate> workingDays(LocalDate from, LocalDate to) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days.add(d);
            }
        }
        return days;
    }
}
