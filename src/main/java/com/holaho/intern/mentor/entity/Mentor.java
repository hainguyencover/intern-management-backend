package com.holaho.intern.mentor.entity;

import com.holaho.intern.entity.Department;
import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
        name = "mentors",
        uniqueConstraints = @UniqueConstraint(name = "uk_mentors_user_id", columnNames = "user_id")
)
public class Mentor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 255)
    private String title;
}

