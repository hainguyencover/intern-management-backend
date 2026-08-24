package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupRetentionService {

    private final BackupJobRepository backupJobRepository;

    @Scheduled(cron = "${app.backup.retention-cron:0 0 3 * * *}") // Run at 3:00 AM daily
    @Transactional
    public void cleanupExpiredBackups() {
        log.info("Running automated backup retention cleanup task");
        LocalDateTime now = LocalDateTime.now();

        List<BackupJob> expiredJobs = backupJobRepository.findByStartedAtBeforeAndStatus(now, "SUCCESS")
                .stream()
                .filter(job -> job.getRetentionUntil() != null && job.getRetentionUntil().isBefore(now))
                .toList();

        int purgedCount = 0;
        for (BackupJob job : expiredJobs) {
            try {
                if (job.getFilePath() != null) {
                    Path file = Paths.get(job.getFilePath());
                    if (Files.exists(file)) {
                        Files.deleteIfExists(file);
                        log.info("Deleted expired backup file: {}", job.getFilePath());
                    }
                }
                job.setStatus("EXPIRED");
                job.setMessage("Backup expired and file removed according to retention policy");
                backupJobRepository.save(job);
                purgedCount++;
            } catch (Exception e) {
                log.error("Failed to cleanup expired backup job ID {}: {}", job.getId(), e.getMessage());
            }
        }

        log.info("Backup retention cleanup completed. Purged {} expired backups.", purgedCount);
    }
}
