package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorWorkloadResponse {

    private Long mentorId;
    private Long userId;
    private String employeeCode;
    private String mentorName;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;
    private String position;
    private String specialization;
    private int currentInternCount;
    private int maxInternCapacity;
    private int utilizationPercent;
    private MentorWorkloadStatus workloadStatus;
    private LocalDateTime lastUpdatedAt;
}
