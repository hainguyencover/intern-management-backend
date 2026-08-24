package com.holaho.intern.integration.entity;

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
    name = "external_identity_mappings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_external_identity",
        columnNames = {"tenant_id", "system_code", "entity_type", "external_id"}
    ),
    indexes = @Index(name = "idx_external_mapping_internal", columnList = "internal_id")
)
public class ExternalIdentityMapping extends BaseEntity {

    @Column(name = "system_code", nullable = false, length = 100)
    private String systemCode;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(name = "internal_id", nullable = false)
    private Long internalId;
}
