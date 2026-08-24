package com.holaho.intern.integration.service;

import com.holaho.intern.entity.Attendance;
import com.holaho.intern.integration.adapter.AttendanceEventDto;
import com.holaho.intern.integration.entity.AttendanceIntegrationEvent;
import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.enums.EventProcessingStatus;
import com.holaho.intern.integration.enums.IntegrationType;
import com.holaho.intern.integration.repository.AttendanceIntegrationEventRepository;
import com.holaho.intern.integration.repository.ExternalIdentityMappingRepository;
import com.holaho.intern.integration.repository.IntegrationConnectionRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceIntegrationServiceTest {

    @Mock private IntegrationConnectionRepository connectionRepository;
    @Mock private AttendanceIntegrationEventRepository eventRepository;
    @Mock private ExternalIdentityMappingRepository identityMappingRepository;
    @Mock private UserRepository userRepository;
    @Mock private InternProfileRepository internProfileRepository;
    @Mock private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AttendanceIntegrationService attendanceIntegrationService;

    private IntegrationConnection testConnection;

    @BeforeEach
    void setUp() {
        testConnection = IntegrationConnection.builder()
                .code("QR_ATTENDANCE")
                .name("QR Gate Scanner")
                .integrationType(IntegrationType.ATTENDANCE_QR)
                .provider("Generic QR")
                .enabled(true)
                .build();
        testConnection.setId(1L);
        testConnection.setTenantId(1L);
    }

    @Test
    void processEvent_shouldDetectDuplicateAndReturnStatusDuplicate() {
        AttendanceEventDto eventDto = AttendanceEventDto.builder()
                .externalEventId("EVT-1001")
                .externalEmployeeId("EMP-001")
                .eventType("CHECK_IN")
                .eventTime(LocalDateTime.now())
                .deviceId("QR-GATE-01")
                .method("QR")
                .build();

        AttendanceIntegrationEvent existingEvent = AttendanceIntegrationEvent.builder()
                .externalEventId("EVT-1001")
                .processingStatus(EventProcessingStatus.PROCESSED)
                .build();

        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));
        when(eventRepository.findByTenantIdAndConnectionIdAndExternalEventId(1L, 1L, "EVT-1001"))
                .thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(AttendanceIntegrationEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceIntegrationEvent result = attendanceIntegrationService.processEvent(1L, eventDto);

        assertThat(result.getProcessingStatus()).isEqualTo(EventProcessingStatus.DUPLICATE);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void processEvent_shouldCreateAttendanceRecordWhenNewEventProcessed() {
        AttendanceEventDto eventDto = AttendanceEventDto.builder()
                .externalEventId("EVT-1002")
                .externalEmployeeId("EMP-001")
                .eventType("CHECK_IN")
                .eventTime(LocalDateTime.of(2026, 8, 24, 8, 5, 0))
                .deviceId("QR-GATE-01")
                .method("QR")
                .build();

        User user = User.builder().fullName("Intern One").build();
        user.setId(50L);
        InternProfile intern = InternProfile.builder().user(user).build();
        intern.setId(10L);

        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));
        when(eventRepository.findByTenantIdAndConnectionIdAndExternalEventId(1L, 1L, "EVT-1002"))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(AttendanceIntegrationEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmail("EMP-001")).thenReturn(Optional.of(user));
        when(internProfileRepository.findByUserId(50L)).thenReturn(Optional.of(intern));
        when(attendanceRepository.findByInternIdAndDate(eq(10L), any())).thenReturn(Optional.empty());

        AttendanceIntegrationEvent result = attendanceIntegrationService.processEvent(1L, eventDto);

        assertThat(result.getProcessingStatus()).isEqualTo(EventProcessingStatus.PROCESSED);
        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }
}
