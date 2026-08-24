package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.MentorStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorResponse {
    private Long id;
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private String employeeCode;
    private Long departmentId;
    private String departmentName;
    private String position;
    private String title;
    private String specialization;
    private BigDecimal yearsOfExperience;
    private Integer capacity;
    private Integer activeInternsCount;
    private Integer availableCapacity;
    private Double capacityUtilizationPercentage;
    private MentorStatus status;
    private String avatarUrl;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
