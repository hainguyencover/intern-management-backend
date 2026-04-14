package com.holaho.intern.entity;

import com.holaho.intern.entity.Department;
import com.holaho.intern.mentor.entity.Mentor;


import com.holaho.intern.shared.entity.BaseEntity;

import com.holaho.intern.shared.enums.GroupStatus;
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

    // KhÃƒÆ’Ã‚Â´ng dÃƒÆ’Ã‚Â¹ng entity Department/Mentor nÃƒÂ¡Ã‚Â»Ã‚Â¯a -> chÃƒÂ¡Ã‚Â»Ã¢â‚¬Â° lÃƒâ€ Ã‚Â°u id
    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "mentor_id")
    private Long mentorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupStatus status;

    @Column(name = "work_start_time")
    private java.time.LocalTime workStartTime;

    @Column(name = "work_end_time")
    private java.time.LocalTime workEndTime;

    @Column(name = "work_days", length = 100)
    private String workDays; // Comma separated: MONDAY,TUESDAY...
}

