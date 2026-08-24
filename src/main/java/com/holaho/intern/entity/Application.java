package com.holaho.intern.entity;

import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;

import com.holaho.intern.shared.entity.BaseEntity;

import com.holaho.intern.shared.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "applications", indexes = @Index(name = "idx_applications_status", columnList = "status"))
public class Application extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(length = 255)
    private String position;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(length = 1000)
    private String note;

    @Column(name = "ai_score")
    private Integer aiScore;

    @Column(name = "ai_skills", length = 1000)
    private String aiSkills; // Comma separated or JSON

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_recommendation", columnDefinition = "TEXT")
    private String aiRecommendation;

    @Version
    private Long version;
}
