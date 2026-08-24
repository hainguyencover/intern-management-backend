package com.holaho.intern.mentor.dto;

import com.holaho.intern.mentor.enums.ProfileStatus;
import com.holaho.intern.shared.enums.MentorStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorProfileDetailResponse {
    private Long id;
    private Long userId;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;

    private Long departmentId;
    private String departmentName;
    private String jobTitle;
    private String position;
    private String title;
    private String specialization;

    private String bio;
    private BigDecimal yearsOfExperience;
    private Integer mentoringExperienceYears;

    private Integer maxInterns;
    private Integer currentInternCount;
    private Integer availableCapacity;

    private MentorStatus status;
    private ProfileStatus profileStatus;

    private List<MentorSkillResponse> skills;
    private List<MentoringDomainResponse> domains;
    private List<MentorExperienceResponse> experiences;
    private List<MentorCertificationResponse> certifications;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
