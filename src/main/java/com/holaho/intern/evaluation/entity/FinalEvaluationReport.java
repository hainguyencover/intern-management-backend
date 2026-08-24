package com.holaho.intern.evaluation.entity;

import com.holaho.intern.entity.Program;
import com.holaho.intern.evaluation.enums.FinalReportStatus;
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
    name = "final_evaluation_reports",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_final_report_intern",
        columnNames = {"tenant_id", "intern_id"}
    ),
    indexes = {
        @Index(name = "idx_final_report_status", columnList = "tenant_id, status"),
        @Index(name = "idx_final_report_program", columnList = "tenant_id, program_id")
    }
)
public class FinalEvaluationReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @Column(name = "report_number", length = 50)
    private String reportNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FinalReportStatus status = FinalReportStatus.DRAFT;

    // ── Snapshot scores (0.00 - 10.00) ──

    @Column(name = "evaluation_score", precision = 5, scale = 2)
    private BigDecimal evaluationScore;

    @Column(name = "task_score", precision = 5, scale = 2)
    private BigDecimal taskScore;

    @Column(name = "attendance_score", precision = 5, scale = 2)
    private BigDecimal attendanceScore;

    @Column(name = "weekly_report_score", precision = 5, scale = 2)
    private BigDecimal weeklyReportScore;

    @Column(name = "final_score", precision = 5, scale = 2)
    private BigDecimal finalScore;

    @Column(length = 30)
    private String classification;

    // ── Snapshot performance stats ──

    @Column(name = "task_total")
    private Integer taskTotal;

    @Column(name = "task_completed")
    private Integer taskCompleted;

    @Column(name = "task_overdue")
    private Integer taskOverdue;

    @Column(name = "task_completion_rate", precision = 5, scale = 2)
    private BigDecimal taskCompletionRate;

    @Column(name = "attendance_total")
    private Integer attendanceTotal;

    @Column(name = "attendance_present")
    private Integer attendancePresent;

    @Column(name = "attendance_absent")
    private Integer attendanceAbsent;

    @Column(name = "attendance_late")
    private Integer attendanceLate;

    @Column(name = "attendance_rate", precision = 5, scale = 2)
    private BigDecimal attendanceRate;

    @Column(name = "report_total")
    private Integer reportTotal;

    @Column(name = "report_submitted")
    private Integer reportSubmitted;

    @Column(name = "report_late")
    private Integer reportLate;

    @Column(name = "report_missing")
    private Integer reportMissing;

    // ── Comments ──

    @Column(name = "mentor_comment", length = 4000)
    private String mentorComment;

    @Column(name = "hr_comment", length = 4000)
    private String hrComment;

    @Column(name = "return_reason", length = 2000)
    private String returnReason;

    // ── Audit Timestamps ──

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FinalReportItem> items = new ArrayList<>();
}
