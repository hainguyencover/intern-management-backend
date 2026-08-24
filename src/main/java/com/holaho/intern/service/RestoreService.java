package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.repository.BackupJobRepository;
import com.holaho.intern.shared.annotation.Auditable;
import com.holaho.intern.shared.util.ChecksumUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestoreService {

    private final BackupJobRepository backupJobRepository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.mysql-client-path:}")
    private String mysqlClientPath;

    @Transactional
    @Auditable(action = "RESTORE_DATABASE", resource = "BACKUP_JOB")
    public void restoreFromBackup(Long backupJobId, String confirmationCode) {
        if (!"CONFIRM_RESTORE".equalsIgnoreCase(confirmationCode)) {
            throw new IllegalArgumentException("Confirmation code 'CONFIRM_RESTORE' is required to execute restore");
        }

        BackupJob job = backupJobRepository.findById(backupJobId)
                .orElseThrow(() -> new IllegalArgumentException("Backup job not found: " + backupJobId));

        if (!"SUCCESS".equals(job.getStatus())) {
            throw new IllegalStateException("Only successful backup jobs can be restored. Current status: " + job.getStatus());
        }

        if (job.getFilePath() == null || !Files.exists(Paths.get(job.getFilePath()))) {
            throw new IllegalStateException("Backup file does not exist on disk: " + job.getFilePath());
        }

        Path backupFile = Paths.get(job.getFilePath());

        // Validate SHA-256 Checksum Safety Gate
        try {
            String currentChecksum = ChecksumUtils.calculateSHA256(backupFile);
            if (job.getChecksum() != null && !job.getChecksum().equalsIgnoreCase(currentChecksum)) {
                throw new IllegalStateException("Backup file checksum mismatch! Recorded: " + job.getChecksum() + ", Calculated: " + currentChecksum);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed integrity checksum validation: " + e.getMessage(), e);
        }

        log.warn("COMMENCING DATABASE RESTORE FOR BACKUP JOB ID: {}", backupJobId);
        Path decompressedSql = null;
        try {
            decompressedSql = decompressGzip(backupFile);
            executeRestoreCommand(decompressedSql);
            log.info("Database restore completed successfully for job ID: {}", backupJobId);
        } catch (Exception e) {
            log.error("Database restore FAILED for job ID: {}", backupJobId, e);
            throw new RuntimeException("Database restore failed: " + e.getMessage(), e);
        } finally {
            if (decompressedSql != null) {
                try {
                    Files.deleteIfExists(decompressedSql);
                } catch (IOException ignored) {}
            }
        }
    }

    private Path decompressGzip(Path source) throws IOException {
        Path tempSql = Files.createTempFile("restore_", ".sql");
        try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(source));
             OutputStream os = Files.newOutputStream(tempSql)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
        }
        return tempSql;
    }

    private void executeRestoreCommand(Path sqlFile) throws Exception {
        String dbName = extractDatabaseName(dbUrl);
        String mysqlTool = (mysqlClientPath != null && !mysqlClientPath.isBlank()) ? mysqlClientPath : "mysql";
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        String command = String.format("\"%s\" --user=%s --password=%s %s < \"%s\"",
                mysqlTool, dbUsername, dbPassword, dbName, sqlFile.toAbsolutePath().toString());

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
                throw new RuntimeException("mysql restore client exit code " + exitCode + ": " + err.toString());
            }
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
