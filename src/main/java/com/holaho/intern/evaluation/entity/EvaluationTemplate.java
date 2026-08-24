package com.holaho.intern.evaluation.entity;

import com.holaho.intern.entity.Program;
import com.holaho.intern.evaluation.enums.EvaluationPeriod;
import com.holaho.intern.evaluation.enums.TemplateStatus;
import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurable evaluation template.
 * Each program can have its own set of criteria.
 * HR can create program-specific templates; a default template
 * is used as fallback for programs without one.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "evaluation_templates",
    indexes = {
        @Index(name = "idx_eval_template_tenant", columnList = "tenant_id"),
        @Index(name = "idx_eval_template_program", columnList = "tenant_id, program_id"),
        @Index(name = "idx_eval_template_status", columnList = "tenant_id, status")
    }
)
public class EvaluationTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_period", nullable = false, length = 20)
    @Builder.Default
    private EvaluationPeriod evaluationPeriod = EvaluationPeriod.FINAL;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.ACTIVE;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<EvaluationCriterion> criteria = new ArrayList<>();

    // ── Helper methods ──

    public boolean isActive() {
        return status == TemplateStatus.ACTIVE;
    }

    /**
     * Validates that all criteria weights sum to 100%.
     */
    public boolean isWeightValid() {
        if (criteria == null || criteria.isEmpty()) return false;
        double totalWeight = criteria.stream()
                .mapToDouble(c -> c.getWeight().doubleValue())
                .sum();
        return Math.abs(totalWeight - 100.0) < 0.01;
    }
}
