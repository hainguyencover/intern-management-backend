package com.holaho.intern.integration.service;

import com.holaho.intern.integration.adapter.DefaultHrmAdapter;
import com.holaho.intern.integration.adapter.HrmEmployeeDto;
import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.entity.IntegrationSyncJob;
import com.holaho.intern.integration.enums.IntegrationType;
import com.holaho.intern.integration.enums.SyncStatus;
import com.holaho.intern.integration.repository.*;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrmSyncServiceTest {

    @Mock private IntegrationConnectionRepository connectionRepository;
    @Mock private IntegrationCredentialRepository credentialRepository;
    @Mock private IntegrationSyncJobRepository syncJobRepository;
    @Mock private IntegrationSyncItemRepository syncItemRepository;
    @Mock private ExternalIdentityMappingRepository identityMappingRepository;
    @Mock private UserRepository userRepository;
    @Mock private InternProfileRepository internProfileRepository;
    @Mock private DefaultHrmAdapter hrmAdapter;

    @InjectMocks
    private HrmSyncService hrmSyncService;

    private IntegrationConnection testConnection;
    private IntegrationSyncJob testJob;

    @BeforeEach
    void setUp() {
        testConnection = IntegrationConnection.builder()
                .code("HRM_MAIN")
                .name("Main HRM System")
                .integrationType(IntegrationType.HRM)
                .provider("MISA")
                .enabled(true)
                .build();
        testConnection.setId(1L);
        testConnection.setTenantId(1L);

        testJob = IntegrationSyncJob.builder()
                .connection(testConnection)
                .syncType("HRM_EMPLOYEE_SYNC")
                .status(SyncStatus.PENDING)
                .build();
        testJob.setId(10L);
        testJob.setTenantId(1L);
    }

    @Test
    void executeHrmSync_shouldProcessEmployeesAndUpdateJobStatusToSuccess() {
        HrmEmployeeDto emp = HrmEmployeeDto.builder()
                .externalId("EMP-001")
                .fullName("Test Intern")
                .email("test.intern@example.com")
                .phone("0912345678")
                .status("ACTIVE")
                .dateOfBirth(LocalDate.of(2002, 1, 1))
                .build();

        when(syncJobRepository.findById(10L)).thenReturn(Optional.of(testJob));
        when(credentialRepository.findByConnectionId(1L)).thenReturn(Collections.emptyList());
        when(hrmAdapter.fetchEmployees(any(), any())).thenReturn(List.of(emp));
        when(identityMappingRepository.findByTenantIdAndSystemCodeAndEntityTypeAndExternalId(1L, "HRM_MAIN", "EMPLOYEE", "EMP-001"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("test.intern@example.com")).thenReturn(Optional.empty());

        User savedUser = User.builder().fullName("Test Intern").email("test.intern@example.com").build();
        savedUser.setId(100L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(syncJobRepository.save(any(IntegrationSyncJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntegrationSyncJob resultJob = hrmSyncService.executeHrmSync(10L);

        assertThat(resultJob.getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(resultJob.getSuccessRecords()).isEqualTo(1);
        assertThat(resultJob.getFailedRecords()).isEqualTo(0);

        verify(identityMappingRepository, times(1)).save(any());
        verify(internProfileRepository, times(1)).save(any());
    }
}
