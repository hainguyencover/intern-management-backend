package com.example.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MentorResponse {
    private Long id;        // mentorId
    private Long userId;

    private String email;
    private String fullName;
    private String phone;

    private String title;
    private DepartmentLite department;

    private LocalDateTime createdAt;

    @Data
    public static class DepartmentLite {
        private Long id;
        private String code;
        private String name;
    }
}
