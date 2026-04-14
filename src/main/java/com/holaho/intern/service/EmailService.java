package com.holaho.intern.service;

import com.holaho.intern.task.entity.Task;


import com.holaho.intern.intern.entity.InternshipContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationResultEmail(String to, String name, String decision, String comment) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Kết quả xét duyệt hồ sơ thực tập");
            message.setText(String.format(
                    "Xin chào %s,\n\n" +
                            "Hồ sơ thực tập của bạn đã được xét duyệt.\n" +
                            "Kết quả: %s\n" +
                            "Nhận xét: %s\n\n" +
                            "Trân trọng,\n" +
                            "Ban quản lý thực tập",
                    name, decision, comment != null ? comment : "Không có"));

            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: " + to, e);
        }
    }

    @Async
    public void sendContractEmail(InternshipContract contract) {
        try {
            String toEmail = contract.getApplication().getIntern().getUser().getEmail();
            String subject = "Hợp đồng thực tập - Vui lòng xác nhận";
            String body = String.format(
                    "Xin chào %s,\n\n" +
                            "Hợp đồng thực tập của bạn đã sẵn sàng.\n" +
                            "Vui lòng đăng nhập vào hệ thống để xem và ký hợp đồng.\n\n" +
                            "Trân trọng,\n" +
                            "Phòng Nhân sự",
                    contract.getApplication().getIntern().getUser().getFullName());

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Contract email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send contract email", e);
            // Don't throw - email failure shouldn't break the flow
        }
    }

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    @Async
    public void sendTaskAssignmentEmail(String to, String internName, String taskTitle, String dueDate,
            String creatorName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Thông báo: Bạn có công việc/lịch họp mới - " + taskTitle);
            message.setText(String.format(
                    "Xin chào %s,\n\n" +
                            "Bạn đã được giao một công việc/lịch họp mới trên hệ thống.\n\n" +
                            "Tiêu đề: %s\n" +
                            "Người giao: %s\n" +
                            "Hạn chót/Thời gian: %s\n\n" +
                            "Vui lòng đăng nhập vào hệ thống để xem chi tiết và thực hiện.\n\n" +
                            "Trân trọng,\n" +
                            "Hệ thống Quản lý Thực tập",
                    internName, taskTitle, creatorName, dueDate != null ? dueDate : "Không có thời hạn"));

            mailSender.send(message);
            log.info("Task assignment email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send task assignment email to: " + to, e);
        }
    }
}
