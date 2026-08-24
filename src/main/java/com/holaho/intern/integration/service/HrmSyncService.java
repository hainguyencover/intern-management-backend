package com.holaho.intern.integration.service;

import com.holaho.intern.integration.adapter.HrmAdapter;
import com.holaho.intern.integration.adapter.HrmEmployeeDto;
import com.holaho.intern.integration.entity.*;
import com.holaho.intern.integration.enums.SyncDirection;
import com.holaho.intern.integration.enums.SyncStatus;
import com.holaho.intern.integration.repository.*;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrmSyncService {

    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationCredentialRepository credentialRepository;
    private final IntegrationSyncJobRepository syncJobRepository;
    private final IntegrationSyncItemRepository syncItemRepository;
    private final ExternalIdentityMappingRepository identityMappingRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final HrmAdapter hrmAdapter;

    @Transactional
    public IntegrationSyncJob startHrmSync(Long connectionId) {
        Long tenantId = getTenantId();
        IntegrationConnection connection = connectionRepository.findById(connectionId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        if (!connection.getEnabled()) {
            throw new IllegalStateException("Integration connection is disabled");
        }

        IntegrationSyncJob job = IntegrationSyncJob.builder()
                .connection(connection)
                .syncType("HRM_EMPLOYEE_SYNC")
                .direction(SyncDirection.INBOUND)
                .status(SyncStatus.PENDING)
                .totalRecords(0)
                .successRecords(0)
                .failedRecords(0)
                .retryCount(0)
                .build();
        job.setTenantId(tenantId);
        return syncJobRepository.save(job);
    }

    @Async
    @Transactional
    public void executeHrmSyncAsync(Long syncJobId) {
        executeHrmSync(syncJobId);
    }

    @Transactional
    public IntegrationSyncJob executeHrmSync(Long syncJobId) {
        IntegrationSyncJob job = syncJobRepository.findById(syncJobId)
                .orElseThrow(() -> new IllegalArgumentException("Sync job not found: " + syncJobId));

        job.setStatus(SyncStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        syncJobRepository.save(job);

        IntegrationConnection connection = job.getConnection();
        Long tenantId = job.getTenantId();
        List<IntegrationCredential> credentials = credentialRepository.findByConnectionId(connection.getId());

        int successCount = 0;
        int failedCount = 0;

        try {
            List<HrmEmployeeDto> employees = hrmAdapter.fetchEmployees(connection, credentials);
            job.setTotalRecords(employees.size());

            for (HrmEmployeeDto emp : employees) {
                try {
                    Long internalUserId = processSingleEmployee(tenantId, connection.getCode(), emp);
                    
                    syncItemRepository.save(IntegrationSyncItem.builder()
                            .syncJob(job)
                            .externalId(emp.getExternalId())
                            .internalId(internalUserId)
                            .entityType("USER")
                            .action("UPSERT")
                            .status(SyncStatus.SUCCESS)
                            .processedAt(LocalDateTime.now())
                            .build());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to sync employee externalId={}: {}", emp.getExternalId(), e.getMessage());
                    syncItemRepository.save(IntegrationSyncItem.builder()
                            .syncJob(job)
                            .externalId(emp.getExternalId())
                            .entityType("USER")
                            .action("UPSERT")
                            .status(SyncStatus.FAILED)
                            .errorCode("UPSERT_ERROR")
                            .errorMessage(e.getMessage())
                            .processedAt(LocalDateTime.now())
                            .build());
                    failedCount++;
                }
            }

            job.setSuccessRecords(successCount);
            job.setFailedRecords(failedCount);
            job.setCompletedAt(LocalDateTime.now());

            if (failedCount == 0) {
                job.setStatus(SyncStatus.SUCCESS);
            } else if (successCount > 0) {
                job.setStatus(SyncStatus.PARTIAL_SUCCESS);
            } else {
                job.setStatus(SyncStatus.FAILED);
            }

            connection.setLastSyncAt(LocalDateTime.now());
            connectionRepository.save(connection);

        } catch (Exception e) {
            log.error("HRM Sync job failed completely: {}", e.getMessage(), e);
            job.setStatus(SyncStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }

        return syncJobRepository.save(job);
    }

    private Long processSingleEmployee(Long tenantId, String systemCode, HrmEmployeeDto emp) {
        Optional<ExternalIdentityMapping> mappingOpt = identityMappingRepository
                .findByTenantIdAndSystemCodeAndEntityTypeAndExternalId(tenantId, systemCode, "EMPLOYEE", emp.getExternalId());

        User user;
        if (mappingOpt.isPresent()) {
            Long userId = mappingOpt.get().getInternalId();
            user = userRepository.findById(userId)
                    .orElseGet(() -> createNewUserAndProfile(tenantId, emp));
            updateUserAndProfile(user, emp);
        } else {
            Optional<User> existingUserOpt = userRepository.findByEmail(emp.getEmail());
            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();
                updateUserAndProfile(user, emp);
            } else {
                user = createNewUserAndProfile(tenantId, emp);
            }

            ExternalIdentityMapping mapping = ExternalIdentityMapping.builder()
                    .systemCode(systemCode)
                    .entityType("EMPLOYEE")
                    .externalId(emp.getExternalId())
                    .internalId(user.getId())
                    .build();
            mapping.setTenantId(tenantId);
            identityMappingRepository.save(mapping);
        }

        return user.getId();
    }

    private User createNewUserAndProfile(Long tenantId, HrmEmployeeDto emp) {
        User user = User.builder()
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .phoneNumber(emp.getPhone())
                .status("ACTIVE")
                .build();
        user.setTenantId(tenantId);
        User savedUser = userRepository.save(user);

        InternProfile profile = InternProfile.builder()
                .user(savedUser)
                .fullName(emp.getFullName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .status("Draft")
                .build();
        profile.setTenantId(tenantId);
        internProfileRepository.save(profile);

        return savedUser;
    }

    private void updateUserAndProfile(User user, HrmEmployeeDto emp) {
        user.setFullName(emp.getFullName());
        if (emp.getPhone() != null) user.setPhoneNumber(emp.getPhone());
        userRepository.save(user);

        Optional<InternProfile> profileOpt = internProfileRepository.findByUserId(user.getId());
        if (profileOpt.isPresent()) {
            InternProfile profile = profileOpt.get();
            profile.setFullName(emp.getFullName());
            if (emp.getPhone() != null) profile.setPhone(emp.getPhone());
            internProfileRepository.save(profile);
        }
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
