package com.example.backend.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InternProfileResponse {
    private Long internId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String studentCode;
    private LocalDate dob;
    private String university;
    private String major;
    private String address;
    private Double gpa;
    private String CvUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long mentorId;
    private MentorResponseDto mentor;
}
