package com.holaho.intern.evaluation.entity;

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
    name = "final_report_items",
    indexes = {
        @Index(name = "idx_report_item", columnList = "report_id")
    }
)
public class FinalReportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private FinalEvaluationReport report;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "item_key", nullable = false, length = 100)
    private String itemKey;

    @Column(name = "item_value", length = 500)
    private String itemValue;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
