package com.holaho.intern.integration.service;

import com.holaho.intern.integration.adapter.AttendanceAdapter;
import com.holaho.intern.integration.adapter.HrmAdapter;
import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationCredential;
import com.holaho.intern.integration.enums.ConnectionStatus;
import com.holaho.intern.integration.enums.IntegrationType;
import com.holaho.intern.integration.repository.IntegrationConnectionRepository;
import com.holaho.intern.integration.repository.IntegrationCredentialRepository;
import com.holaho.intern.shared.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationConnectionService {

    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationCredentialRepository credentialRepository;
    private final IntegrationCredentialCryptoService cryptoService;
    private final HrmAdapter hrmAdapter;
    private final AttendanceAdapter attendanceAdapter;

    public List<IntegrationConnection> getAllConnections() {
        Long tenantId = getTenantId();
        return connectionRepository.findByTenantId(tenantId);
    }

    public Optional<IntegrationConnection> getConnectionById(Long id) {
        Long tenantId = getTenantId();
        return connectionRepository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId));
    }

    @Transactional
    public IntegrationConnection createConnection(
            String code, String name, IntegrationType integrationType,
            String provider, String baseUrl, String authType, Map<String, String> credentials
    ) {
        Long tenantId = getTenantId();
        if (connectionRepository.findByTenantIdAndCode(tenantId, code).isPresent()) {
            throw new IllegalArgumentException("Integration connection code already exists: " + code);
        }

        IntegrationConnection connection = IntegrationConnection.builder()
                .code(code)
                .name(name)
                .integrationType(integrationType)
                .provider(provider)
                .baseUrl(baseUrl)
                .authType(authType)
                .status(ConnectionStatus.INACTIVE)
                .enabled(true)
                .build();
        connection.setTenantId(tenantId);
        IntegrationConnection savedConnection = connectionRepository.save(connection);

        saveCredentials(savedConnection, credentials);
        return savedConnection;
    }

    @Transactional
    public IntegrationConnection updateConnection(
            Long id, String name, String baseUrl, String authType,
            Boolean enabled, Map<String, String> credentials
    ) {
        IntegrationConnection connection = getConnectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Integration connection not found: " + id));

        if (name != null) connection.setName(name);
        if (baseUrl != null) connection.setBaseUrl(baseUrl);
        if (authType != null) connection.setAuthType(authType);
        if (enabled != null) connection.setEnabled(enabled);

        if (credentials != null && !credentials.isEmpty()) {
            saveCredentials(connection, credentials);
        }

        return connectionRepository.save(connection);
    }

    @Transactional
    public void deleteConnection(Long id) {
        IntegrationConnection connection = getConnectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Integration connection not found: " + id));
        credentialRepository.deleteByConnectionId(connection.getId());
        connectionRepository.delete(connection);
    }

    @Transactional
    public boolean testConnection(Long id) {
        IntegrationConnection connection = getConnectionById(id)
                .orElseThrow(() -> new IllegalArgumentException("Integration connection not found: " + id));

        List<IntegrationCredential> credentials = credentialRepository.findByConnectionId(id);

        boolean success = false;
        long startTime = System.currentTimeMillis();

        if (connection.getIntegrationType() == IntegrationType.HRM) {
            success = hrmAdapter.testConnection(connection, credentials);
        } else {
            success = attendanceAdapter.testConnection(connection, credentials);
        }

        long latency = System.currentTimeMillis() - startTime;
        log.info("Connection test result for {}: success={}, latency={}ms", connection.getCode(), success, latency);

        connection.setStatus(success ? ConnectionStatus.ACTIVE : ConnectionStatus.ERROR);
        connectionRepository.save(connection);
        return success;
    }

    private void saveCredentials(IntegrationConnection connection, Map<String, String> credentials) {
        if (credentials == null || credentials.isEmpty()) return;

        for (Map.Entry<String, String> entry : credentials.entrySet()) {
            String key = entry.getKey();
            String rawVal = entry.getValue();
            if (rawVal == null || rawVal.isEmpty()) continue;

            String encryptedVal = cryptoService.encrypt(rawVal);
            Optional<IntegrationCredential> existing = credentialRepository
                    .findByConnectionIdAndCredentialKey(connection.getId(), key);

            if (existing.isPresent()) {
                IntegrationCredential cred = existing.get();
                cred.setEncryptedValue(encryptedVal);
                credentialRepository.save(cred);
            } else {
                IntegrationCredential cred = IntegrationCredential.builder()
                        .connection(connection)
                        .credentialKey(key)
                        .encryptedValue(encryptedVal)
                        .build();
                credentialRepository.save(cred);
            }
        }
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
