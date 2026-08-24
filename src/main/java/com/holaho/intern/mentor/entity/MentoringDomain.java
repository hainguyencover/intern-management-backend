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
        name = "mentoring_domains",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_domain_tenant_name", columnNames = {"tenant_id", "name"})
        },
        indexes = {
                @Index(name = "idx_domain_name", columnList = "name")
        }
)
public class MentoringDomain extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
