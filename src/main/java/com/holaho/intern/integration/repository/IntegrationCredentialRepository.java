package com.holaho.intern.integration.repository;

import com.holaho.intern.integration.entity.IntegrationCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationCredentialRepository extends JpaRepository<IntegrationCredential, Long> {

    List<IntegrationCredential> findByConnectionId(Long connectionId);

    Optional<IntegrationCredential> findByConnectionIdAndCredentialKey(Long connectionId, String credentialKey);

    void deleteByConnectionId(Long connectionId);
}
