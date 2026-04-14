package com.holaho.intern.intern.entity;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "intern_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_intern_profiles_user_id", columnNames = "user_id"))
public class InternProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    private LocalDate dob;

    @Column(length = 255)
    private String university;

    @Column(length = 255)
    private String major;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    private Double gpa;

    @Column(name = "cv_url")
    private String cvUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "cv_skills", length = 1000)
    private String cvSkills;

    @Column(name = "cv_score")
    private Integer cvScore;

    @Column(name = "cv_summary", columnDefinition = "TEXT")
    private String cvSummary;
}

