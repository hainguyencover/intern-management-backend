package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weekly_reports", indexes = {
                @Index(name = "idx_reports_intern_week", columnList = "intern_id,week_number"),
                @Index(name = "idx_reports_status", columnList = "status")
})
public class WeeklyReport extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "intern_id", nullable = false)
        private InternProfile intern;

        @Column(name = "week_number", nullable = false)
        private Integer weekNumber;

        @Column(nullable = false)
        private String title;

        @Column(name = "week_start", nullable = false)
        private LocalDate weekStart;

        @Column(name = "week_end", nullable = false)
        private LocalDate weekEnd;

        @Column(name = "report_date", nullable = false)
        private LocalDate reportDate;

        @Column(name = "completed_work", nullable = false, columnDefinition = "TEXT")
        private String completedWork;

        @Column(name = "planned_work", columnDefinition = "TEXT")
        private String plannedWork;

        @Column(columnDefinition = "TEXT")
        private String challenges;

        @Column(columnDefinition = "TEXT")
        private String learnings;

        @Column(name = "file_url")
        private String fileUrl = "";

        @Column(name = "mentor_feedback", columnDefinition = "TEXT")
        private String mentorFeedback;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "mentor_id")
        private User mentor;

        @Column(nullable = false, length = 20)
        private String status = "DRAFT"; // DRAFT, SUBMITTED, REVIEWED

        @Column(name = "submitted_at")
        private LocalDateTime submittedAt;

        @Column(name = "reviewed_at")
        private LocalDateTime reviewedAt;
}
