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

    // Không dùng entity Department/Mentor nữa -> chỉ lưu id
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "mentor_id")
    private Long mentorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status;
}
