package com.holaho.intern.mentor.dto;

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
public class MentorActiveInternResponse {

    private Long assignmentId;
    private Long internId;
    private String internCode;
    private String fullName;
    private String email;
    private String phone;
    private String university;
    private String programName;
    private String assignmentType;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime assignedAt;
    private String note;
}
