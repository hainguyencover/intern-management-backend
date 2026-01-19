package com.example.backend.repository;

import com.example.backend.entity.BackupJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {

    List<BackupJob> findByStatusOrderByStartedAtDesc(String status);

    List<BackupJob> findTop30ByOrderByStartedAtDesc();

    List<BackupJob> findByStartedAtBeforeAndStatus(LocalDateTime before, String status);

    @Query("SELECT bj FROM BackupJob bj WHERE bj.startedAt < :cutoffDate")
    List<BackupJob> findOldBackups(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT b FROM BackupJob b ORDER BY b.startedAt DESC")
    Page<BackupJob> findAllOrderByStartedAtDesc(Pageable pageable);

    @Modifying
    @Query("DELETE FROM BackupJob b WHERE b.status = 'SUCCESS' AND b.startedAt < :cutoffDate")
    void deleteOldSuccessfulBackups(@Param("cutoffDate") LocalDateTime cutoffDate);
}
