package com.holaho.intern.attendance.repository;

import com.holaho.intern.attendance.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    List<LeaveType> findByTenantIdAndIsActiveTrue(Long tenantId);
}
