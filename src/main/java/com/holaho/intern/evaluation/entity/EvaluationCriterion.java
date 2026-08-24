package com.holaho.intern.evaluation.entity;

import com.holaho.intern.evaluation.enums.CriteriaCategory;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single evaluation criterion belonging to a template.
 * Defines what is being assessed, its weight, and max score.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "evaluation_criteria",
    indexes = {
        @Index(name = "idx_eval_criteria_template", columnList = "template_id"),
        @Index(name = "idx_eval_criteria_category", columnList = "template_id, category")
    }
)
public class EvaluationCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private EvaluationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CriteriaCategory category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    /**
     * Weight as percentage (e.g. 15.00 = 15%).
     * All criteria weights in a template must sum to 100.
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "max_score", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal maxScore = new BigDecimal("10.00");

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
