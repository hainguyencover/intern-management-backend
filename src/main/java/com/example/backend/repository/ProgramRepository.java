package com.example.backend.repository;

import com.example.backend.entity.Program;
import com.example.backend.enums.ProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Page<Program> findAllByDepartment_Id(Long departmentId, Pageable pageable);

    Page<Program> findAllByStatus(ProgramStatus status, Pageable pageable);

    Page<Program> findAllByDepartment_IdAndStatus(Long departmentId, ProgramStatus status, Pageable pageable);

    boolean existsByDepartment_Id(Long departmentId);
}
