package com.holaho.intern.entity;

import com.holaho.intern.entity.InternProfile;


import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task_updates")
public class TaskUpdate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Column(name = "progress_percent")
    private Integer progressPercent; // 0..100

    @Column(length = 4000)
    private String content;
}

