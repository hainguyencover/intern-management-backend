package com.holaho.intern.shared.aspect;

import com.holaho.intern.shared.events.AuditEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        when(joinPoint.getTarget()).thenReturn(new DummyUserService());
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("createUser");
    }

    @Test
    @DisplayName("Should publish AuditEvent on method success and mask sensitive fields")
    void logAfterMethodSuccess_PublishesEventAndMasksPassword() {
        Map<String, Object> createUserRequest = new HashMap<>();
        createUserRequest.put("username", "testuser");
        createUserRequest.put("password", "secret123");

        when(joinPoint.getArgs()).thenReturn(new Object[]{createUserRequest});

        auditAspect.logAfterMethodSuccess(joinPoint, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        AuditEvent event = captor.getValue();
        assertNotNull(event);
        assertEquals("CREATE", event.getAction());
        assertEquals("USER", event.getResourceType());
        assertEquals("SUCCESS", event.getResult());

        assertNotNull(event.getBeforeJson());
        assertTrue(event.getBeforeJson().contains("[REDACTED]"));
        assertFalse(event.getBeforeJson().contains("secret123"));
    }

    // Dummy target class for JoinPoint target
    private static class DummyUserService {}
}
