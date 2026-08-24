package com.holaho.intern.report.entity;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "weekly_report_feedbacks",
    indexes = {
        @Index(name = "idx_feedback_report", columnList = "weekly_report_id"),
        @Index(name = "idx_feedback_mentor", columnList = "tenant_id, mentor_id")
    }
)
public class WeeklyReportFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_report_id", nullable = false)
    private WeeklyReport weeklyReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Version
    private Long version;
}
