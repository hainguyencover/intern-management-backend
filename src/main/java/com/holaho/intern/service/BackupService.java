package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.repository.BackupJobRepository;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private final BackupJobRepository backupJobRepository;
    private final UserRepository userRepository;

    @Value("${app.backup.dir:backups}")
    private String backupDir;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.tool-path:}")
    private String backupToolPath;

    @Transactional
    public BackupJob runManualBackup(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return executeBackup("MANUAL", user);
    }

    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}") // Default: 2:00 AM daily
    @Transactional
    public void runScheduledBackup() {
        log.info("Starting scheduled backup");
        executeBackup("SCHEDULED", null);
    }

    private BackupJob executeBackup(String type, User createdBy) {
        BackupJob job = new BackupJob();
        job.setType(type);
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        job.setCreatedBy(createdBy);

        job = backupJobRepository.save(job);

        try {
            Path backupPath = Paths.get(backupDir);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "backup_" + timestamp + ".sql";
            String filePath = backupPath.resolve(filename).toString();

            // Extract database name from JDBC URL
            String dbName = extractDatabaseName(dbUrl);

            // Determine OS and command wrapper
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb;

            // Resolve tool path
            String dumpTool = (backupToolPath != null && !backupToolPath.isBlank()) ? backupToolPath : "mysqldump";

            // Construct mysqldump command
            String command = String.format("\"%s\" --user=%s --password=%s --result-file=\"%s\" %s",
                    dumpTool, dbUsername, dbPassword, filePath, dbName);

            if (isWindows) {
                // On Windows, use cmd /c to handle potential path issues or shell reliance
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                // On Linux/Mac, use sh -c
                pb = new ProcessBuilder("sh", "-c", command);
            }

            log.info("Executing backup command: {}", isWindows ? "cmd /c ...mysqldump..." : "sh -c ...mysqldump...");

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                long fileSize = Files.size(Paths.get(filePath));

                job.setStatus("SUCCESS");
                job.setFilePath(filePath);
                job.setFileSize(fileSize);
                job.setFinishedAt(LocalDateTime.now());
                job.setMessage("Backup completed successfully");

                log.info("Backup completed: {}", filename);
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorMsg = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    errorMsg.append(line).append("\n");
                }

                job.setStatus("FAILED");
                job.setFinishedAt(LocalDateTime.now());
                job.setMessage("Backup failed: " + errorMsg.toString());

                log.error("Backup failed: {}", errorMsg.toString());
            }

        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setFinishedAt(LocalDateTime.now());
            job.setMessage("Backup failed: " + e.getMessage());

            log.error("Backup failed", e);
        }

        return backupJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Page<BackupJob> getBackupHistory(Pageable pageable) {
        return backupJobRepository.findAllOrderByStartedAtDesc(pageable);
    }

    @Transactional
    public void cleanupOldBackups(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        backupJobRepository.deleteOldSuccessfulBackups(cutoffDate);
        log.info("Cleaned up backups older than {} days", retentionDays);
    }

    private String extractDatabaseName(String jdbcUrl) {
        // Example: jdbc:mysql://localhost:3306/ims_db
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?', lastSlash);

        if (questionMark > 0) {
            return jdbcUrl.substring(lastSlash + 1, questionMark);
        } else {
            return jdbcUrl.substring(lastSlash + 1);
        }
    }
}

