package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.dto.CorrectionResponse;
import com.holaho.intern.attendance.dto.CreateCorrectionRequestDto;
import com.holaho.intern.attendance.dto.ReviewCorrectionRequestDto;
import com.holaho.intern.attendance.entity.AttendanceCorrection;
import com.holaho.intern.attendance.enums.AttendanceStatus;
import com.holaho.intern.attendance.enums.CorrectionStatus;
import com.holaho.intern.attendance.repository.AttendanceCorrectionRepository;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceCorrectionService {

    private final AttendanceCorrectionRepository correctionRepository;
    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final AttendanceCoreService coreService;

    @Transactional
    public CorrectionResponse createCorrectionRequest(Long internId, CreateCorrectionRequestDto req) {
        Long tenantId = getCurrentTenantId();

        Attendance attendance = attendanceRepository.findById(req.getAttendanceId())
                .orElseThrow(() -> new NotFoundException("Attendance", req.getAttendanceId()));

        if (!attendance.getIntern().getId().equals(internId)) {
            throw new BadRequestException("Bản ghi chấm công không thuộc về bạn.");
        }

        // Check if pending correction already exists
        if (correctionRepository.existsByAttendanceIdAndStatus(attendance.getId(), CorrectionStatus.PENDING)) {
            throw new ConflictException("Yêu cầu sửa công cho ngày này đang chờ HR duyệt.");
        }

        AttendanceCorrection correction = AttendanceCorrection.builder()
                .attendance(attendance)
                .intern(attendance.getIntern())
                .requestedCheckIn(req.getRequestedCheckIn())
                .requestedCheckOut(req.getRequestedCheckOut())
                .reason(req.getReason())
                .status(CorrectionStatus.PENDING)
                .build();
        correction.setTenantId(tenantId);

        // Update attendance status to PENDING_CORRECTION
        attendance.setStatus(AttendanceStatus.PENDING_CORRECTION.name());
        attendanceRepository.save(attendance);

        correction = correctionRepository.save(correction);
        log.info("Created attendance correction request ID {} for intern {}", correction.getId(), internId);

        return mapToResponse(correction);
    }

    @Transactional
    public CorrectionResponse approveCorrection(Long correctionId, Long hrUserId, ReviewCorrectionRequestDto req) {
        AttendanceCorrection correction = correctionRepository.findById(correctionId)
                .orElseThrow(() -> new NotFoundException("Correction request", correctionId));

        if (correction.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException("Yêu cầu sửa công không ở trạng thái chờ duyệt.");
        }

        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("User", hrUserId));

        correction.setStatus(CorrectionStatus.APPROVED);
        correction.setReviewedBy(hrUser);
        correction.setReviewedAt(LocalDateTime.now());
        if (req != null && req.getReviewComment() != null) {
            correction.setReviewComment(req.getReviewComment());
        }

        // Apply corrected timestamps to Attendance record
        Attendance attendance = correction.getAttendance();
        if (correction.getRequestedCheckIn() != null) {
            attendance.setCheckIn(correction.getRequestedCheckIn());
        }
        if (correction.getRequestedCheckOut() != null) {
            attendance.setCheckOut(correction.getRequestedCheckOut());
        }

        // Reset status to PRESENT
        attendance.setStatus(AttendanceStatus.PRESENT.name());
        attendanceRepository.save(attendance);

        correction = correctionRepository.save(correction);
        log.info("Approved attendance correction ID {} by HR {}", correctionId, hrUserId);

        return mapToResponse(correction);
    }

    @Transactional
    public CorrectionResponse rejectCorrection(Long correctionId, Long hrUserId, ReviewCorrectionRequestDto req) {
        AttendanceCorrection correction = correctionRepository.findById(correctionId)
                .orElseThrow(() -> new NotFoundException("Correction request", correctionId));

        if (correction.getStatus() != CorrectionStatus.PENDING) {
            throw new BadRequestException("Yêu cầu sửa công không ở trạng thái chờ duyệt.");
        }

        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("User", hrUserId));

        correction.setStatus(CorrectionStatus.REJECTED);
        correction.setReviewedBy(hrUser);
        correction.setReviewedAt(LocalDateTime.now());
        if (req != null && req.getReviewComment() != null) {
            correction.setReviewComment(req.getReviewComment());
        }

        // Reset attendance status back from PENDING_CORRECTION to INCOMPLETE / PRESENT
        Attendance attendance = correction.getAttendance();
        if (attendance.getCheckOut() == null) {
            attendance.setStatus(AttendanceStatus.INCOMPLETE.name());
        } else {
            attendance.setStatus(AttendanceStatus.PRESENT.name());
        }
        attendanceRepository.save(attendance);

        correction = correctionRepository.save(correction);
        log.info("Rejected attendance correction ID {} by HR {}", correctionId, hrUserId);

        return mapToResponse(correction);
    }

    @Transactional(readOnly = true)
    public Page<CorrectionResponse> getCorrections(CorrectionStatus status, Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        if (status != null) {
            return correctionRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(this::mapToResponse);
        }
        return correctionRepository.findByTenantId(tenantId, pageable).map(this::mapToResponse);
    }

    private CorrectionResponse mapToResponse(AttendanceCorrection c) {
        return CorrectionResponse.builder()
                .id(c.getId())
                .attendanceId(c.getAttendance() != null ? c.getAttendance().getId() : null)
                .attendanceDate(c.getAttendance() != null ? c.getAttendance().getDate() : null)
                .internId(c.getIntern() != null ? c.getIntern().getId() : null)
                .internName(c.getIntern() != null && c.getIntern().getUser() != null ? c.getIntern().getUser().getFullName() : null)
                .studentCode(c.getIntern() != null ? c.getIntern().getStudentCode() : null)
                .currentCheckIn(c.getAttendance() != null ? c.getAttendance().getCheckIn() : null)
                .currentCheckOut(c.getAttendance() != null ? c.getAttendance().getCheckOut() : null)
                .requestedCheckIn(c.getRequestedCheckIn())
                .requestedCheckOut(c.getRequestedCheckOut())
                .reason(c.getReason())
                .status(c.getStatus().name())
                .reviewedByName(c.getReviewedBy() != null ? c.getReviewedBy().getFullName() : null)
                .reviewedAt(c.getReviewedAt())
                .reviewComment(c.getReviewComment())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
