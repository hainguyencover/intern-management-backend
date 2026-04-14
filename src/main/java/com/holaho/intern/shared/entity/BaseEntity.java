package com.holaho.intern.shared.entity;

import com.holaho.intern.user.entity.User;


import com.holaho.intern.shared.config.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Renamed to avoid collision with business relationships like "createdBy"
    // (User) in subclasses
    @Column(name = "audit_created_by")
    private String auditCreatedBy;

    @Column(name = "audit_updated_by")
    private String auditUpdatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null)
            createdAt = now;
        if (updatedAt == null)
            updatedAt = now;
        // Auto-set tenant from context if not explicitly set
        if (tenantId == null) {
            Long contextTenantId = TenantContext.getCurrentTenantId();
            tenantId = (contextTenantId != null) ? contextTenantId : 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

