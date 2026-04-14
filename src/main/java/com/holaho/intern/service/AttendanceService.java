package com.holaho.intern.service;

import com.holaho.intern.shared.dto.request.QrLogDto;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;


import com.holaho.intern.shared.dto.response.AttendanceResponse;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;

    @Transactional
    public AttendanceResponse checkIn(Long internId) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        LocalDate today = LocalDate.now();
        if (attendanceRepository.existsByInternIdAndDate(internId, today)) {
            throw new ConflictException("Bạn đã chấm công vào hôm nay rồi");
        }

        Attendance attendance = new Attendance();
        attendance.setIntern(intern);
        attendance.setDate(today);
        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckIn(now);

        // Check LATE logic (Work starts at 08:30)
        LocalTime workStartTime = LocalTime.of(8, 30);
        if (now.toLocalTime().isAfter(workStartTime)) {
            attendance.setStatus("LATE");
        } else {
            attendance.setStatus("PRESENT");
        }

        attendance = attendanceRepository.save(attendance);
        log.info("Intern {} checked in", internId);

        return mapToResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(Long internId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByInternIdAndDate(internId, today)
                .orElseThrow(() -> new NotFoundException("Chưa có bản ghi chấm công vào hôm nay"));

        if (attendance.getCheckOut() != null) {
            throw new ConflictException("Bạn đã chấm công ra hôm nay rồi");
        }

        LocalDateTime checkOut = LocalDateTime.now();
        attendance.setCheckOut(checkOut);

        long minutes = ChronoUnit.MINUTES.between(attendance.getCheckIn(), checkOut);
        attendance.setTotalMinutes((int) minutes);

        attendance = attendanceRepository.save(attendance);
        log.info("Intern {} checked out", internId);

        return mapToResponse(attendance);
    }

    @Transactional(readOnly = true)
    public Attendance getTodayAttendance(Long internId) {
        return attendanceRepository.findByInternIdAndDate(internId, LocalDate.now())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByInternId(Long internId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByInternIdAndDateBetween(internId, startDate, endDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Attendance> getAttendanceHistory(
            Long internId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        if (fromDate == null)
            fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null)
            toDate = LocalDate.now();

        return attendanceRepository.findByInternAndDateRange(internId, fromDate, toDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Attendance> getAllAttendance(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        if (fromDate == null)
            fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null)
            toDate = LocalDate.now();

        return attendanceRepository.findByDateRange(fromDate, toDate, pageable);
    }

    @Transactional
    public String syncQrData(List<com.holaho.intern.shared.dto.request.QrLogDto> logs) {
        int processed = 0;
        int errors = 0;

        for (com.holaho.intern.shared.dto.request.QrLogDto qrLog : logs) {
            try {
                // Try to find intern by Email (assuming employeeCode = email for this demo)
                // In real world, use studentCode or a specific cardId
                InternProfile intern = internProfileRepository.findByUser_Email(qrLog.getEmployeeCode())
                        .orElse(null);

                if (intern == null) {
                    // Try by studentCode? -> Need repo method. Skipping for now.
                    log.error("Intern not found for code: {}", qrLog.getEmployeeCode());
                    errors++;
                    continue;
                }

                LocalDate date = qrLog.getTimestamp().toLocalDate();
                Attendance attendance = attendanceRepository.findByInternIdAndDate(intern.getId(), date)
                        .orElse(new Attendance());

                if (attendance.getId() == null) {
                    attendance.setIntern(intern);
                    attendance.setDate(date);
                    attendance.setCheckIn(qrLog.getTimestamp());
                    attendance.setNote("Synced from Device " + qrLog.getDeviceId());
                } else {
                    // If exists, verify times.
                    // If log time is earlier than checkIn -> update checkIn
                    // If log time is later than checkIn -> update checkOut (or create checkOut)
                    if (qrLog.getTimestamp().isBefore(attendance.getCheckIn())) {
                        attendance.setCheckIn(qrLog.getTimestamp());
                    } else if (attendance.getCheckOut() == null
                            || qrLog.getTimestamp().isAfter(attendance.getCheckOut())) {
                        attendance.setCheckOut(qrLog.getTimestamp());

                        // Recalculate duration
                        long minutes = ChronoUnit.MINUTES.between(attendance.getCheckIn(), qrLog.getTimestamp());
                        attendance.setTotalMinutes((int) minutes);
                    }
                }

                attendanceRepository.save(attendance);
                processed++;

            } catch (Exception e) {
                log.error("Error processing QR log", e);
                errors++;
            }
        }
        return "Processed: " + processed + ", Errors: " + errors;
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .internId(attendance.getIntern().getId())
                .internName(attendance.getIntern().getUser().getFullName())
                .date(attendance.getDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .totalMinutes(attendance.getTotalMinutes())
                .note(attendance.getNote())
                .status(attendance.getStatus())
                .build();
    }
}

