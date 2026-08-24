package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.enums.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {

    List<IntegrationConnection> findByTenantId(Long tenantId);

    Optional<IntegrationConnection> findByTenantIdAndCode(Long tenantId, String code);

    List<IntegrationConnection> findByTenantIdAndIntegrationType(Long tenantId, IntegrationType integrationType);

    List<IntegrationConnection> findByEnabledTrueAndIntegrationType(IntegrationType integrationType);
}
