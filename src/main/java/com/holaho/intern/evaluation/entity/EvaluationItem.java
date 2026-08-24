package com.holaho.intern.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A scored item within an evaluation, corresponding to one criterion.
 * Stores snapshot of weight and max_score at the time of evaluation
 * to ensure historical accuracy when template criteria change.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "evaluation_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_eval_item",
        columnNames = {"evaluation_id", "criterion_id"}
    ),
    indexes = {
        @Index(name = "idx_eval_item_evaluation", columnList = "evaluation_id")
    }
)
public class EvaluationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false, insertable = false, updatable = false)
    private Long evaluationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private com.holaho.intern.entity.Evaluation evaluation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criterion_id", nullable = false)
    private EvaluationCriterion criterion;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(length = 2000)
    private String comment;

    /**
     * Snapshot of the criterion's weight at the time the evaluation was created.
     * This ensures that even if the template weight is later changed,
     * historical evaluations retain their original scoring.
     */
    @Column(name = "weight_snapshot", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightSnapshot;

    @Column(name = "max_score_snapshot", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal maxScoreSnapshot = new BigDecimal("10.00");

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate the weighted contribution of this item to the overall score.
     * Formula: (score / maxScore) * weight
     * But since we score on a 0-10 scale and weight is percentage:
     * contribution = score * (weight / 100)
     */
    public BigDecimal getWeightedContribution() {
        if (score == null || weightSnapshot == null) return BigDecimal.ZERO;
        return score.multiply(weightSnapshot).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
    }
}
