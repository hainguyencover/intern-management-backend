package com.holaho.intern.mentor.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "skills",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_skill_tenant_name", columnNames = {"tenant_id", "name"})
        },
        indexes = {
                @Index(name = "idx_skill_name", columnList = "name"),
                @Index(name = "idx_skill_category", columnList = "category")
        }
)
public class Skill extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String category;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
