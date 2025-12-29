package com.example.backend.entity;

import com.example.backend.enums.ReviewDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "application_reviews")
public class ApplicationReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(length = 1000)
    private String comment;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
