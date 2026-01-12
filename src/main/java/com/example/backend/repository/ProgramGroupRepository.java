package com.example.backend.repository;

import com.example.backend.entity.ProgramGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramGroupRepository extends JpaRepository<ProgramGroup, Long> {
    List<ProgramGroup> findByProgram_Id(Long programId);
}
