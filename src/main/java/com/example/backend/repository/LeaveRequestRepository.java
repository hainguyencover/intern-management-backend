package com.example.backend.repository;

import com.example.backend.entity.LeaveRequest;
import com.example.backend.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "approvedBy" })
        Optional<LeaveRequest> findById(Long id);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "approvedBy" })
        Page<LeaveRequest> findByIntern_Id(Long internId, Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "approvedBy" })
        Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "intern", "intern.user", "approvedBy" })
        @Query("SELECT lr FROM LeaveRequest lr WHERE " +
                        "(:internId IS NULL OR lr.intern.id = :internId) AND " +
                        "(:status IS NULL OR lr.status = :status)")
        Page<LeaveRequest> search(
                        @Param("internId") Long internId,
                        @Param("status") LeaveStatus status,
                        Pageable pageable);

        @Query("SELECT COUNT(lr) > 0 FROM LeaveRequest lr WHERE " +
                        "lr.intern.id = :internId AND " +
                        "lr.status IN ('PENDING', 'APPROVED') AND " +
                        "((:startDate BETWEEN lr.startDate AND lr.endDate) OR " +
                        " (:endDate BETWEEN lr.startDate AND lr.endDate) OR " +
                        " (lr.startDate BETWEEN :startDate AND :endDate))")
        boolean existsByInternAndDateOverlap(
                        @Param("internId") Long internId,
                        @Param("startDate") java.time.LocalDate startDate,
                        @Param("endDate") java.time.LocalDate endDate);
}
