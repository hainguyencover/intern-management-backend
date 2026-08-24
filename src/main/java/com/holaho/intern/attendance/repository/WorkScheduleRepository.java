package com.holaho.intern.attendance.repository;

import com.holaho.intern.attendance.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    Optional<WorkSchedule> findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(Long tenantId);
    List<WorkSchedule> findByTenantId(Long tenantId);
}
