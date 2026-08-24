package com.holaho.intern.integration.entity;

import com.holaho.intern.integration.enums.ConnectionStatus;
import com.holaho.intern.integration.enums.IntegrationType;
import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "integration_connections",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_integration_connection",
        columnNames = {"tenant_id", "code"}
    )
)
public class IntegrationConnection extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 50)
    private IntegrationType integrationType;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.INACTIVE;

    @Column(name = "auth_type", length = 30)
    private String authType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
}
