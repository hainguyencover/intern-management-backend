package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternProfileResponse {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String studentCode;
    private LocalDate dob;
    private String university;
    private String major;
    private String address;
    private Double gpa;
    private String cvUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long mentorId;
    private String mentorName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long programGroupId;
    private String programGroupName;
    private String cvSkills;
    private Integer cvScore;
    private String cvSummary;
}
