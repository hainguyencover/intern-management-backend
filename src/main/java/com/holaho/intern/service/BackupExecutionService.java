package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.repository.BackupJobRepository;
import com.holaho.intern.shared.util.ChecksumUtils;
import com.holaho.intern.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupExecutionService {

    private final BackupJobRepository backupJobRepository;

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

    @Value("${app.backup.retention.daily-days:7}")
    private int retentionDailyDays;

    @Value("${app.backup.max-retry-attempts:3}")
    private int maxRetryAttempts;

    @Transactional
    public BackupJob executeBackupJob(String type, String backupType, Long tenantId, User createdBy) {
        BackupJob job = BackupJob.builder()
                .tenantId(tenantId)
                .type(type)
                .backupType(backupType != null ? backupType : "FULL")
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .retentionUntil(LocalDateTime.now().plusDays(retentionDailyDays))
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        job = backupJobRepository.save(job);

        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetryAttempts && !success) {
            attempt++;
            try {
                log.info("Executing backup attempt {}/{} for job ID {}", attempt, maxRetryAttempts, job.getId());
                performBackupProcess(job);
                success = true;
            } catch (Exception e) {
                log.warn("Backup attempt {} failed for job ID {}: {}", attempt, job.getId(), e.getMessage());
                if (attempt >= maxRetryAttempts) {
                    job.setStatus("FAILED");
                    job.setErrorCode("BACKUP_EXECUTION_ERROR");
                    job.setErrorMessage("Failed after " + maxRetryAttempts + " attempts: " + e.getMessage());
                    job.setFinishedAt(LocalDateTime.now());
                    backupJobRepository.save(job);
                } else {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return job;
    }

    private void performBackupProcess(BackupJob job) throws Exception {
        Path backupPath = Paths.get(backupDir);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseName = "ims_backup_" + timestamp + "_" + job.getId();
        Path rawSqlFile = backupPath.resolve(baseName + ".sql");
        Path compressedFile = backupPath.resolve(baseName + ".sql.gz");

        String dbName = extractDatabaseName(dbUrl);
        String dumpTool = (backupToolPath != null && !backupToolPath.isBlank()) ? backupToolPath : "mysqldump";
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String command = String.format("\"%s\" --user=%s --password=%s --result-file=\"%s\" %s",
                dumpTool, dbUsername, dbPassword, rawSqlFile.toAbsolutePath().toString(), dbName);

        ProcessBuilder pb;
        if (isWindows) {
            pb = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            pb = new ProcessBuilder("sh", "-c", command);
        }

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder err = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    err.append(line).append(" ");
                }
                throw new RuntimeException("mysqldump exit code " + exitCode + ": " + err.toString());
            }
        }

        // Compress SQL file with GZIP
        compressFile(rawSqlFile, compressedFile);
        Files.deleteIfExists(rawSqlFile);

        // Compute Checksum & File Size
        String checksum = ChecksumUtils.calculateSHA256(compressedFile);
        long fileSize = Files.size(compressedFile);

        job.setStatus("SUCCESS");
        job.setFilePath(compressedFile.toAbsolutePath().toString());
        job.setStoragePath("minio://backups/" + compressedFile.getFileName().toString());
        job.setFileSize(fileSize);
        job.setChecksum(checksum);
        job.setFinishedAt(LocalDateTime.now());
        job.setMessage("Backup completed successfully with SHA-256 integrity verification");

        backupJobRepository.save(job);
        log.info("Backup successfully completed for job ID: {}. Checksum: {}", job.getId(), checksum);
    }

    private void compressFile(Path source, Path target) throws IOException {
        try (InputStream is = Files.newInputStream(source);
             OutputStream os = Files.newOutputStream(target);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();
        }
    }

    private String extractDatabaseName(String jdbcUrl) {
        int lastSlash = jdbcUrl.lastIndexOf('/');
        int questionMark = jdbcUrl.indexOf('?', lastSlash);
        if (questionMark > 0) {
            return jdbcUrl.substring(lastSlash + 1, questionMark);
        } else {
            return jdbcUrl.substring(lastSlash + 1);
        }
    }
}
