package com.holaho.intern.service;

import com.holaho.intern.entity.BackupJob;
import com.holaho.intern.repository.BackupJobRepository;
import com.holaho.intern.shared.dto.response.BackupJobResponse;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertsyntax.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupJobRepository backupJobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BackupExecutionService backupExecutionService;

    @Mock
    private RestoreService restoreService;

    @InjectMocks
    private BackupService backupService;

    private User mockUser;
    private BackupJob mockJob;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");

        mockJob = BackupJob.builder()
                .id(100L)
                .tenantId(1L)
                .backupType("FULL")
                .type("MANUAL")
                .status("SUCCESS")
                .filePath("/backups/test.sql.gz")
                .checksum("abcd1234efgh5678")
                .startedAt(LocalDateTime.now())
                .createdBy(mockUser)
                .build();
    }

    @Test
    @DisplayName("Should run manual backup successfully")
    void runManualBackup_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(backupExecutionService.executeBackupJob(eq("MANUAL"), eq("FULL"), any(), eq(mockUser))).thenReturn(mockJob);

        BackupJobResponse response = backupService.runManualBackup(1L, "FULL");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("admin", response.getCreatedByUsername());
        verify(backupExecutionService, times(1)).executeBackupJob(eq("MANUAL"), eq("FULL"), any(), eq(mockUser));
    }

    @Test
    @DisplayName("Should retrieve backup history page")
    void getBackupHistory_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BackupJob> page = new PageImpl<>(List.of(mockJob));
        when(backupJobRepository.findAllOrderByStartedAtDesc(pageable)).thenReturn(page);

        Page<BackupJobResponse> result = backupService.getBackupHistory(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("SUCCESS", result.getContent().get(0).getStatus());
    }

    @Test
    @DisplayName("Should trigger restore with confirmation code")
    void restoreBackup_Success() {
        doNothing().when(restoreService).restoreFromBackup(100L, "CONFIRM_RESTORE");

        assertDoesNotThrow(() -> backupService.restoreBackup(100L, "CONFIRM_RESTORE"));
        verify(restoreService, times(1)).restoreFromBackup(100L, "CONFIRM_RESTORE");
    }
}
