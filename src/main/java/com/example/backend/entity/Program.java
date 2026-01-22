package com.example.backend.entity;

import com.example.backend.enums.ProgramStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "programs", indexes = {
                @Index(name = "idx_programs_status", columnList = "status"),
                @Index(name = "idx_programs_department_status", columnList = "department_id,status")
})
public class Program extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "department_id", nullable = false)
        @NotFound(action = NotFoundAction.IGNORE)
        private Department department;

        @Column(nullable = false, length = 255)
        private String name;

        @Column(length = 2000)
        private String description;

        @Column(name = "start_date")
        private LocalDate startDate;

        @Column(name = "end_date")
        private LocalDate endDate;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private ProgramStatus status = ProgramStatus.ACTIVE;
}
