package com.holaho.intern.listener;

import com.holaho.intern.entity.EmailLog;
import com.holaho.intern.repository.EmailLogRepository;
import com.holaho.intern.service.EmailService;
import com.holaho.intern.shared.events.ApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationSubmittedEventListener {

    private final EmailService emailService;
    private final EmailLogRepository emailLogRepository;

    @Async
    @EventListener
    public void handleApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Handling ApplicationSubmittedEvent for app id: {}, user: {}", event.getApplicationId(), event.getEmail());

        EmailLog logEntry = EmailLog.builder()
                .recipient(event.getEmail())
                .templateCode("APPLICATION_SUBMITTED")
                .referenceType("APPLICATION")
                .referenceId(event.getApplicationId())
                .build();

        try {
            emailService.sendApplicationSubmittedEmail(
                    event.getEmail(),
                    event.getFullName(),
                    event.getProgramName(),
                    String.valueOf(event.getApplicationId())
            );

            logEntry.setStatus("SUCCESS");
            logEntry.setSentAt(LocalDateTime.now());
            log.info("Successfully queued/sent application submission email to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send application submission email to: {}", event.getEmail(), e);
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(e.getMessage());
        } finally {
            try {
                emailLogRepository.save(logEntry);
            } catch (Exception ex) {
                log.error("Failed to save EmailLog for application submission", ex);
            }
        }
    }
}
