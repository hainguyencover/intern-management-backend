package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.AuditLogResponse;
import com.holaho.intern.entity.AuditLog;
import com.holaho.intern.shared.mapper.AuditLogMapper;
import com.holaho.intern.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private AuditLog auditLog;
    private AuditLogResponse auditLogResponse;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setAction("CREATE");
        auditLog.setEntityType("USER");

        auditLogResponse = new AuditLogResponse();
        auditLogResponse.setId(1L);
        auditLogResponse.setAction("CREATE");
        auditLogResponse.setEntityType("USER");
    }

    @Test
    void createAuditLog_ShouldSaveLog() {
        auditLogService.createAuditLog(1L, "admin@example.com", "CREATE", "USER", 100L, "SUCCESS", "Created user", "{}",
                "{}", "127.0.0.1", "Mozilla");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("CREATE", saved.getAction());
        assertEquals("USER", saved.getEntityType());
        assertEquals(100L, saved.getEntityId());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAuditLogs_ShouldReturnPageOfResponses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(Collections.singletonList(auditLog));

        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(auditLogMapper.toResponse(any(AuditLog.class))).thenReturn(auditLogResponse);

        Page<AuditLogResponse> result = auditLogService.getAuditLogs(null, null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("CREATE", result.getContent().get(0).getAction());
    }

    @Test
    void getAuditLogById_ShouldReturnResponse() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(auditLog));
        when(auditLogMapper.toResponse(auditLog)).thenReturn(auditLogResponse);

        AuditLogResponse result = auditLogService.getAuditLogById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}

