package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evaluations")
public class Evaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Column(length = 50)
    private String period;

    @Column(name = "skill_score")
    private Integer skillScore; // 0-10

    @Column(name = "attitude_score")
    private Integer attitudeScore; // 0-10

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(length = 2000)
    private String comment;
}
