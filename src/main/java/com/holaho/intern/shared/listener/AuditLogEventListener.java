package com.holaho.intern.shared.listener;

import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.shared.events.AuditEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogEventListener {

    private final AuditLogService auditLogService;
    private final MeterRegistry meterRegistry;
    private static final int MAX_RETRIES = 3;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleAuditEvent(AuditEvent event) {
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
            attempt++;
            try {
                log.debug("Processing async AuditEvent (Attempt {}/{}) for {} on {}",
                        attempt, MAX_RETRIES, event.getAction(), event.getEntityType());

                auditLogService.createAuditLog(event);
                success = true;
            } catch (Exception e) {
                log.warn("Failed attempt {}/{} to process AuditEvent: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        long jitter = ThreadLocalRandom.current().nextLong(50);
                        long sleepTime = 100L * attempt + jitter;
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    meterRegistry.counter("audit.fallback.count", "action", event.getAction()).increment();
                    log.error("[EMERGENCY_AUDIT_FALLBACK] Failed to persist audit log to DB after {} attempts. Event: actor={}, action={}, entity={}, id={}, msg={}",
                            MAX_RETRIES, event.getActorEmail(), event.getAction(), event.getEntityType(), event.getEntityId(), event.getMessage(), e);
                }
            }
        }
    }
}
