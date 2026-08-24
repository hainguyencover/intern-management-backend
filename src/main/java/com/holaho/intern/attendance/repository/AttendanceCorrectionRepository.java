package com.holaho.intern.attendance.repository;

import com.holaho.intern.attendance.entity.AttendanceCorrection;
import com.holaho.intern.attendance.enums.CorrectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection, Long> {

    List<AttendanceCorrection> findByInternIdOrderByCreatedAtDesc(Long internId);

    Page<AttendanceCorrection> findByTenantIdAndStatus(Long tenantId, CorrectionStatus status, Pageable pageable);

    Page<AttendanceCorrection> findByTenantId(Long tenantId, Pageable pageable);

    boolean existsByAttendanceIdAndStatus(Long attendanceId, CorrectionStatus status);
}
