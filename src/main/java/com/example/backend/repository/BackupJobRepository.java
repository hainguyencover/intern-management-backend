package com.example.backend.repository;

import com.example.backend.entity.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {

    List<BackupJob> findByStatusOrderByStartedAtDesc(String status);

    List<BackupJob> findTop30ByOrderByStartedAtDesc();

    List<BackupJob> findByStartedAtBeforeAndStatus(LocalDateTime before, String status);
}
