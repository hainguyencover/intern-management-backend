package com.holaho.intern.mentor.entity;

import com.holaho.intern.entity.Department;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "mentors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mentors_user_id", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_mentor_employee_code", columnNames = {"tenant_id", "employee_code"})
        },
        indexes = {
                @Index(name = "idx_mentor_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_mentor_department", columnList = "tenant_id, department_id")
        }
)
public class Mentor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 100)
    private String position;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String specialization;

    @Column(name = "years_of_experience", precision = 5, scale = 2)
    private BigDecimal yearsOfExperience;

    @Builder.Default
    @Column(nullable = false)
    private Integer capacity = 5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MentorStatus status = MentorStatus.ACTIVE;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    @Column(name = "mentoring_experience_years", nullable = false)
    private Integer mentoringExperienceYears = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false, length = 30)
    private com.holaho.intern.mentor.enums.ProfileStatus profileStatus = com.holaho.intern.mentor.enums.ProfileStatus.DRAFT;

    @Version
    private Long version;
}
