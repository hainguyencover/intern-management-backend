package com.holaho.intern.report.entity;

import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.report.enums.WeeklyReportStatus;
import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "ReportWeeklyReport")
@Table(
    name = "weekly_reports",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_weekly_report_intern_week",
            columnNames = {"tenant_id", "intern_id", "week_start_date"}
        )
    },
    indexes = {
        @Index(name = "idx_weekly_report_intern", columnList = "tenant_id, intern_id"),
        @Index(name = "idx_weekly_report_mentor", columnList = "tenant_id, mentor_id"),
        @Index(name = "idx_weekly_report_status", columnList = "tenant_id, status"),
        @Index(name = "idx_weekly_report_week", columnList = "tenant_id, week_start_date, week_end_date")
    }
)
public class WeeklyReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(length = 255)
    private String title;

    @Column(name = "work_summary", nullable = false, columnDefinition = "TEXT")
    private String workSummary;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Column(columnDefinition = "TEXT")
    private String challenges;

    @Column(name = "next_week_plan", columnDefinition = "TEXT")
    private String nextWeekPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private WeeklyReportStatus status = WeeklyReportStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "is_late", nullable = false)
    private boolean late;

    @OneToMany(mappedBy = "weeklyReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WeeklyReportFeedback> feedbacks = new ArrayList<>();

    @Version
    private Long version;
}
