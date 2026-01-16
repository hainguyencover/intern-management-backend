package com.example.backend.service;

import com.example.backend.dto.response.BackupJobResponse;
import com.example.backend.entity.BackupJob;
import com.example.backend.entity.User;
import com.example.backend.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupJobRepository backupJobRepository;

    @Value("${app.backup.path:./backups}")
    private String backupPath;

    @Value("${app.backup.retention-days:7}")
    private int retentionDays;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Transactional
    public BackupJobResponse runBackupManually(User user) {
        return executeBackup("MANUAL", user);
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}") // 2:00 AM daily
    @Transactional
    public void runScheduledBackup() {
        log.info("Starting scheduled backup...");
        executeBackup("SCHEDULED", null);
        cleanOldBackups();
    }

    private BackupJobResponse executeBackup(String type, User user) {
        BackupJob job = new BackupJob();
        job.setType(type);
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        job.setCreatedBy(user);
        job = backupJobRepository.save(job);

        try {
            // Create backup directory if not exists
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // Generate filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("backup_%s.sql", timestamp);
            String filePath = Paths.get(backupPath, filename).toString();

            // Extract database name from URL
            String dbName = extractDbName(dbUrl);

            // Execute mysqldump
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-u", dbUsername,
                    "-p" + dbPassword,
                    "--single-transaction",
                    "--quick",
                    "--lock-tables=false",
                    dbName
            );
            pb.redirectOutput(new File(filePath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Success
                File backupFile = new File(filePath);
                job.setStatus("SUCCESS");
                job.setFilePath(filePath);
                job.setFileSize(backupFile.length());
                job.setMessage("Backup completed successfully");
            } else {
                job.setStatus("FAILED");
                job.setMessage("mysqldump failed with exit code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Backup failed", e);
            job.setStatus("FAILED");
            job.setMessage("Error: " + e.getMessage());
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            backupJobRepository.save(job);
        }

        return mapToResponse(job);
    }

    private void cleanOldBackups() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<BackupJob> oldJobs = backupJobRepository.findByStartedAtBeforeAndStatus(cutoffDate, "SUCCESS");

        for (BackupJob job : oldJobs) {
            try {
                if (job.getFilePath() != null) {
                    Files.deleteIfExists(Paths.get(job.getFilePath()));
                }
                job.setStatus("DELETED");
                backupJobRepository.save(job);
            } catch (IOException e) {
                log.error("Failed to delete old backup: " + job.getFilePath(), e);
            }
        }
    }

    public List<BackupJobResponse> getBackupHistory() {
        return backupJobRepository.findTop30ByOrderByStartedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BackupJobResponse mapToResponse(BackupJob job) {
        BackupJobResponse response = new BackupJobResponse();
        response.setId(job.getId());
        response.setType(job.getType());
        response.setStatus(job.getStatus());
        response.setFilePath(job.getFilePath());
        response.setFileSize(job.getFileSize());
        response.setStartedAt(job.getStartedAt());
        response.setFinishedAt(job.getFinishedAt());
        response.setMessage(job.getMessage());
        return response;
    }

    private String extractDbName(String url) {
        // Extract database name from jdbc:mysql://localhost:3306/ims_db
        int lastSlash = url.lastIndexOf('/');
        int questionMark = url.indexOf('?', lastSlash);
        if (questionMark > 0) {
            return url.substring(lastSlash + 1, questionMark);
        }
        return url.substring(lastSlash + 1);
    }
}
