package com.holaho.intern.scheduler;

import com.holaho.intern.service.BackupExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final BackupExecutionService backupExecutionService;
    private final StringRedisTemplate redisTemplate;

    private static final String BACKUP_LOCK_KEY = "lock:backup:scheduled_execution";
    private static final Duration LOCK_EXPIRATION = Duration.ofMinutes(15);

    @Scheduled(cron = "${app.backup.schedule:0 0 2 * * *}") // Daily at 02:00 AM
    public void executeScheduledBackup() {
        Boolean acquired = false;
        try {
            acquired = redisTemplate.opsForValue().setIfAbsent(BACKUP_LOCK_KEY, "LOCKED", LOCK_EXPIRATION);
        } catch (Exception e) {
            log.warn("Redis unavailable for backup lock, proceeding with single execution: {}", e.getMessage());
            acquired = true;
        }

        if (Boolean.TRUE.equals(acquired)) {
            log.info("Acquired distributed lock. Starting scheduled backup...");
            try {
                backupExecutionService.executeBackupJob("SCHEDULED", "FULL", null, null);
            } finally {
                try {
                    redisTemplate.delete(BACKUP_LOCK_KEY);
                } catch (Exception ignored) {}
            }
        } else {
            log.info("Scheduled backup lock already acquired by another instance. Skipping.");
        }
    }
}
