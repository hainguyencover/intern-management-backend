package com.holaho.intern.entity;

import com.holaho.intern.evaluation.entity.EvaluationItem;
import com.holaho.intern.evaluation.entity.EvaluationTemplate;
import com.holaho.intern.evaluation.enums.EvaluationStatus;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "evaluations",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_eval_intern_period",
        columnNames = {"tenant_id", "intern_id", "period"}
    ),
    indexes = {
        @Index(name = "idx_eval_intern", columnList = "tenant_id, intern_id"),
        @Index(name = "idx_eval_mentor", columnList = "tenant_id, mentor_id")
    }
)
public class Evaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private EvaluationTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(length = 50)
    private String period;

    // ── State Machine ──

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    // ── Scores (backend-calculated) ──

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "classification", length = 30)
    private String classification;

    @Column(name = "overall_comment", length = 4000)
    private String overallComment;

    // ── Items (scored criteria) ──

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationItem> items = new ArrayList<>();

    // ── Timestamp Trail ──

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "return_reason", length = 2000)
    private String returnReason;

    // ── Legacy Fields (deprecated, kept for backward compatibility) ──

    /** @deprecated Use overallScore calculated from items instead */
    private Integer score;

    /** @deprecated Use EvaluationItem with TECHNICAL criteria */
    @Column(name = "technical_score")
    private Double technicalScore;

    /** @deprecated Use EvaluationItem with TECHNICAL criteria */
    @Column(name = "work_quality_score")
    private Double workQualityScore;

    /** @deprecated Use EvaluationItem with ATTITUDE criteria */
    @Column(name = "attitude_score")
    private Double attitudeScore;

    /** @deprecated Use EvaluationItem with SOFT_SKILL criteria */
    @Column(name = "soft_skill_score")
    private Double softSkillScore;

    /** @deprecated Use overallScore instead */
    @Column(name = "weighted_score")
    private Double weightedScore;

    /** @deprecated Use classification instead */
    @Column(name = "result_status", length = 20)
    @Builder.Default
    private String resultStatus = "PASS";

    /** @deprecated Use overallComment instead */
    @Column(length = 2000)
    private String comment;

    // ── Optimistic Locking ──

    @Version
    private Long version;

    // ── Helper Methods ──

    public boolean isEditable() {
        return status != null && status.isEditable();
    }

    public boolean isLocked() {
        return status != null && status.isLocked();
    }
}
