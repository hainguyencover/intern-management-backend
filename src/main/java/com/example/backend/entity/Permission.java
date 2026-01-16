package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_code", columnNames = "code")
)
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String code; // INTERN_READ, TASK_ASSIGN,...

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 50)
    private String module; // USER, TASK, SYSTEM, etc.

    @Column(length = 500)
    private String description;
}
