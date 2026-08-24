package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.ExternalIdentityMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExternalIdentityMappingRepository extends JpaRepository<ExternalIdentityMapping, Long> {

    Optional<ExternalIdentityMapping> findByTenantIdAndSystemCodeAndEntityTypeAndExternalId(
        Long tenantId, String systemCode, String entityType, String externalId
    );

    Optional<ExternalIdentityMapping> findByTenantIdAndSystemCodeAndEntityTypeAndInternalId(
        Long tenantId, String systemCode, String entityType, Long internalId
    );
}
