package com.example.backend.service.impl;

import com.example.backend.dto.request.CheckOutRequest;
import com.example.backend.dto.response.AttendanceResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.InternProfile;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.service.AttendanceService;
import com.example.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;

    @Override
    @Transactional
    public AttendanceResponse checkIn() {
        InternProfile intern = getCurrentIntern();

        LocalDate today = LocalDate.now();
        Attendance att = attendanceRepository.findByIntern_IdAndDate(intern.getId(), today).orElse(null);

        // đã có record hôm nay và đã check-in
        if (att != null && att.getCheckIn() != null) {
            throw new BadRequestException("You already checked in today");
        }

        if (att == null) {
            att = new Attendance();
            att.setIntern(intern);
            att.setDate(today);
        }

        att.setCheckIn(LocalDateTime.now());
        att = attendanceRepository.save(att);

        return toResponse(att);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(CheckOutRequest req) {
        InternProfile intern = getCurrentIntern();

        LocalDate today = LocalDate.now();
        Attendance att = attendanceRepository.findByIntern_IdAndDate(intern.getId(), today)
                .orElseThrow(() -> new BadRequestException("You have not checked in today"));

        if (att.getCheckIn() == null) {
            throw new BadRequestException("You have not checked in today");
        }

        if (att.getCheckOut() != null) {
            throw new BadRequestException("You already checked out today");
        }

        att.setCheckOut(LocalDateTime.now());

        // totalMinutes
        long minutes = ChronoUnit.MINUTES.between(att.getCheckIn(), att.getCheckOut());
        if (minutes < 0) minutes = 0;
        att.setTotalMinutes((int) minutes);

        // note optional
        if (req != null && req.getNote() != null) {
            att.setNote(req.getNote().trim());
        }

        att = attendanceRepository.save(att);
        return toResponse(att);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse today() {
        InternProfile intern = getCurrentIntern();
        LocalDate today = LocalDate.now();

        return attendanceRepository.findByIntern_IdAndDate(intern.getId(), today)
                .map(this::toResponse)
                .orElse(AttendanceResponse.builder()
                        .internId(intern.getId())
                        .date(today)
                        .status("NOT_CHECKED_IN")
                        .totalMinutes(0)
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> history(int month, int year) {
        InternProfile intern = getCurrentIntern();

        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        return attendanceRepository.findByIntern_IdAndDateBetween(intern.getId(), from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ===== helpers =====
    private InternProfile getCurrentIntern() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return internProfileRepository.findByUser_Email(email)
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));
    }

    private AttendanceResponse toResponse(Attendance a) {
        String status;
        if (a.getCheckIn() == null) status = "NOT_CHECKED_IN";
        else if (a.getCheckOut() == null) status = "CHECKED_IN";
        else status = "CHECKED_OUT";

        return AttendanceResponse.builder()
                .id(a.getId())
                .internId(a.getIntern() != null ? a.getIntern().getId() : null)
                .date(a.getDate())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .totalMinutes(a.getTotalMinutes())
                .note(a.getNote())
                .status(status)
                .build();
    }
}
