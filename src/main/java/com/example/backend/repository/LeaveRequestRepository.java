package com.example.backend.repository;

import com.example.backend.entity.LeaveRequest;
import com.example.backend.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Lấy leave APPROVED có giao với khoảng [from..to]
    @Query("""
        SELECT lr
        FROM LeaveRequest lr
        WHERE lr.status = :status
          AND lr.fromDate <= :to
          AND lr.toDate >= :from
    """)
    List<LeaveRequest> findByStatusOverlappingRange(LeaveStatus status, LocalDate from, LocalDate to);

    @Query("""
        SELECT lr
        FROM LeaveRequest lr
        WHERE lr.intern.id = :internId
          AND lr.status = :status
          AND lr.fromDate <= :to
          AND lr.toDate >= :from
    """)
    List<LeaveRequest> findByInternAndStatusOverlappingRange(Long internId, LeaveStatus status, LocalDate from, LocalDate to);
}
