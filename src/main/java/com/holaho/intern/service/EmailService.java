package com.holaho.intern.service;

import com.holaho.intern.intern.entity.InternshipContract;
import com.holaho.intern.notification.entity.EmailQueue;
import com.holaho.intern.notification.repository.EmailQueueRepository;
import com.holaho.intern.notification.service.EmailTemplateEngine;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailQueueRepository emailQueueRepository;
    private final EmailTemplateEngine templateEngine;

    /**
     * Enqueue a raw simple email
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void queueEmail(String recipient, String subject, String body) {
        try {
            EmailQueue eq = new EmailQueue();
            eq.setRecipient(recipient);
            eq.setSubject(subject);
            eq.setBody(body);
            eq.setStatus("PENDING");
            emailQueueRepository.save(eq);
            log.info("Enqueued email to: {}", recipient);
        } catch (Exception e) {
            log.error("Failed to enqueue email to: {}", recipient, e);
        }
    }

    /**
     * Send email via JavaMailSender (Helper method)
     */
    private void sendMailImmediately(EmailQueue eq) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(eq.getRecipient());
        helper.setSubject(eq.getSubject());
        helper.setText(eq.getBody(), true); // Send as HTML
        mailSender.send(mimeMessage);
    }

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Scheduled worker to process email queue
     */
    @Scheduled(fixedDelay = 5000)
    public void processEmailQueue() {
        List<EmailQueue> pendingEmails = emailQueueRepository.findByStatus("PENDING");
        for (EmailQueue eq : pendingEmails) {
            if (mailUsername == null || mailUsername.isBlank()) {
                eq.setStatus("SENT_DEV_MOCK");
                emailQueueRepository.save(eq);
                log.info("[DEV MOCK EMAIL] Mail credentials empty. Email enqueued for {} subject '{}' marked as SENT_DEV_MOCK", eq.getRecipient(), eq.getSubject());
                continue;
            }
            try {
                sendMailImmediately(eq);
                eq.setStatus("SENT");
                emailQueueRepository.save(eq);
                log.info("Email queue sent successfully to: {}", eq.getRecipient());
            } catch (Exception e) {
                log.warn("Could not send queued email to: {} ({})", eq.getRecipient(), e.getMessage());
                eq.setRetryCount(eq.getRetryCount() + 1);
                eq.setLastError(e.getMessage());
                if (eq.getRetryCount() >= 3) {
                    eq.setStatus("FAILED");
                }
                emailQueueRepository.save(eq);
            }
        }
    }

    public void sendApplicationResultEmail(String to, String name, String decision, String comment) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("decision", decision);
        vars.put("comment", comment != null ? comment : "Không có");
        String htmlBody = templateEngine.render("accepted", vars);
        queueEmail(to, "Kết quả xét duyệt hồ sơ thực tập", htmlBody);
    }

    public void sendContractEmail(InternshipContract contract) {
        String toEmail = contract.getApplication().getIntern().getUser().getEmail();
        String name = contract.getApplication().getIntern().getUser().getFullName();
        String body = String.format(
                "<p>Xin chào %s,</p>" +
                        "<p>Hợp đồng thực tập của bạn đã sẵn sàng.</p>" +
                        "<p>Vui lòng đăng nhập vào hệ thống để xem và ký hợp đồng.</p>",
                name);
        queueEmail(toEmail, "Hợp đồng thực tập - Vui lòng xác nhận", body);
    }

    public void sendSimpleEmail(String to, String subject, String body) {
        queueEmail(to, subject, body);
    }

    public void sendTaskAssignmentEmail(String to, String internName, String taskTitle, String dueDate, String creatorName) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", internName);
        vars.put("taskTitle", taskTitle);
        vars.put("creatorName", creatorName);
        vars.put("dueDate", dueDate != null ? dueDate : "Không có thời hạn");
        String htmlBody = templateEngine.render("task-assigned", vars);
        queueEmail(to, "Thông báo: Bạn có công việc/lịch họp mới - " + taskTitle, htmlBody);
    }

    public void sendAccountCreatedEmail(String to, String name, String tempPassword) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("email", to);
        vars.put("tempPassword", tempPassword);
        String htmlBody = templateEngine.render("welcome", vars);
        queueEmail(to, "Chào mừng bạn đến với HoLaHo Intern Management", htmlBody);
    }

    public void sendPasswordResetEmail(String to, String name, String token) {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("resetLink", "https://holaho.com/reset-password?token=" + token);
        String htmlBody = templateEngine.render("password-reset", vars);
        queueEmail(to, "Yêu cầu đặt lại mật khẩu - HoLaHo Intern Management", htmlBody);
    }

    public void sendEmailVerificationToken(String to, String name, String token) {
        String body = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2>Xác thực địa chỉ Email</h2>" +
                "<p>Xin chào <strong>%s</strong>,</p>" +
                "<p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>HoLaHo IMS</strong>.</p>" +
                "<p>Vui lòng sử dụng mã/token xác thực sau hoặc click vào liên kết bên dưới để xác thực email của bạn:</p>" +
                "<div style=\"background: #f4f6f8; padding: 15px; border-radius: 8px; font-weight: bold; font-size: 18px; text-align: center; letter-spacing: 2px; margin: 20px 0;\">%s</div>" +
                "<p>Mã có hiệu lực trong vòng 24 giờ.</p>" +
                "<br/><p>Trân trọng,<br/>Đội ngũ HoLaHo IMS</p>" +
                "</div>",
                name, token);
        queueEmail(to, "[HoLaHo IMS] Xác thực địa chỉ Email đăng ký", body);
    }

    public void sendApplicationSubmittedEmail(String to, String name, String programName, String applicationId) {
        String body = String.format(
                "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;\">" +
                "<h2>Xác nhận đã nhận hồ sơ thực tập</h2>" +
                "<p>Xin chào <strong>%s</strong>,</p>" +
                "<p>Hệ thống đã nhận hồ sơ đăng ký tham gia chương trình thực tập của bạn:</p>" +
                "<ul>" +
                "  <li><strong>Chương trình:</strong> %s</li>" +
                "  <li><strong>Mã hồ sơ:</strong> APP-%s</li>" +
                "  <li><strong>Trạng thái:</strong> SUBMITTED (Đã nộp)</li>" +
                "</ul>" +
                "<p>Bạn có thể đăng nhập vào hệ thống bất kỳ lúc nào để theo dõi tiến trình xét duyệt hồ sơ.</p>" +
                "<br/><p>Trân trọng,<br/>HoLaHo IMS</p>" +
                "</div>",
                name, programName, applicationId);
        queueEmail(to, "[HoLaHo IMS] Xác nhận đã nhận hồ sơ thực tập", body);
    }
}
