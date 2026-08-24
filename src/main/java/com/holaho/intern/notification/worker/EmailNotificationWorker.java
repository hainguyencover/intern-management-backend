package com.holaho.intern.notification.worker;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.entity.NotificationDelivery;
import com.holaho.intern.notification.enums.DeliveryStatus;
import com.holaho.intern.notification.enums.NotificationChannel;
import com.holaho.intern.notification.repository.NotificationDeliveryRepository;
import com.holaho.intern.notification.service.EmailTemplateEngine;
import com.holaho.intern.notification.service.NotificationDeliveryService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationWorker {

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryService deliveryService;
    private final EmailTemplateEngine templateEngine;
    private final Optional<JavaMailSender> mailSender;

    @Scheduled(fixedDelay = 5000)
    public void processPendingEmails() {
        List<NotificationDelivery> pendingDeliveries = deliveryRepository.findByStatusAndChannel(
                DeliveryStatus.PENDING, NotificationChannel.EMAIL, PageRequest.of(0, 50)
        );

        if (pendingDeliveries.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending email notifications...", pendingDeliveries.size());

        for (NotificationDelivery delivery : pendingDeliveries) {
            try {
                deliveryService.markProcessing(delivery);

                Notification notification = delivery.getNotification();
                String recipientEmail = notification.getRecipient().getEmail();

                if (recipientEmail == null || recipientEmail.isBlank()) {
                    deliveryService.markFailed(delivery, "Recipient has no email address.");
                    continue;
                }

                String templateName = resolveTemplateName(notification.getType().name());
                Map<String, String> variables = buildVariables(notification);

                String htmlContent = templateEngine.render(templateName, variables);

                if (mailSender.isPresent()) {
                    MimeMessage mimeMessage = mailSender.get().createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                    helper.setTo(recipientEmail);
                    helper.setSubject(notification.getTitle());
                    helper.setText(htmlContent, true);
                    mailSender.get().send(mimeMessage);
                    log.info("Successfully sent email to {} for notification ID {}", recipientEmail, notification.getId());
                } else {
                    log.info("[MOCK EMAIL] To: {}, Subject: '{}', Body rendering OK", recipientEmail, notification.getTitle());
                }

                deliveryService.markSent(delivery);
            } catch (Exception ex) {
                log.error("Failed to deliver email for notification ID {}: {}", delivery.getNotification().getId(), ex.getMessage());
                deliveryService.markFailed(delivery, ex.getMessage());
            }
        }
    }

    private String resolveTemplateName(String type) {
        return switch (type) {
            case "MEETING_CREATED" -> "meeting-created";
            case "MEETING_UPDATED" -> "meeting-updated";
            case "MEETING_CANCELLED" -> "meeting-cancelled";
            case "MEETING_REMINDER" -> "meeting-reminder";
            default -> "meeting-created";
        };
    }

    private Map<String, String> buildVariables(Notification notification) {
        Map<String, String> vars = new HashMap<>();
        vars.put("internName", notification.getRecipient().getFullName() != null ? notification.getRecipient().getFullName() : "Thực tập sinh");
        vars.put("meetingTitle", notification.getTitle());
        vars.put("organizerName", "Ban Tổ Chức");
        vars.put("meetingDate", "Hôm nay");
        vars.put("meetingTime", "Sắp diễn ra");
        vars.put("duration", "60");
        vars.put("meetingUrl", "/meetings/" + (notification.getReferenceId() != null ? notification.getReferenceId() : ""));
        vars.put("reason", notification.getMessage());
        return vars;
    }
}
