package com.holaho.intern.shared.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignMentorResponse {
    private Long internId;
    private Long mentorId;
    private String mentorEmail;
    private String mentorFullName;
    private Long mentorDepartmentId;
    private String mentorDepartmentName;
    private LocalDateTime assignedAt;
}

