package com.example.backend.entity;

import com.example.backend.enums.GroupStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "program_groups")
public class ProgramGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id")
    private Mentor mentor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status = GroupStatus.ACTIVE;
}
