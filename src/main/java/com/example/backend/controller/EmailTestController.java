package com.example.backend.controller;

import com.example.backend.service.EmailService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping
    public String send(@RequestBody TestEmailRequest req) {
        emailService.sendMeetingEmail(req.getTo(), req.getSubject(), req.getContent());
        return "OK";
    }

    @Data
    public static class TestEmailRequest {
        private String to;
        private String subject;
        private String content;
    }
}
