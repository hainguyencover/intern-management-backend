package com.holaho.intern.integration.service;

import com.holaho.intern.entity.Attendance;
import com.holaho.intern.integration.adapter.AttendanceEventDto;
import com.holaho.intern.integration.entity.AttendanceIntegrationEvent;
import com.holaho.intern.integration.entity.ExternalIdentityMapping;
import com.holaho.intern.integration.entity.IntegrationConnection;
import com.holaho.intern.integration.enums.EventProcessingStatus;
import com.holaho.intern.integration.repository.AttendanceIntegrationEventRepository;
import com.holaho.intern.integration.repository.ExternalIdentityMappingRepository;
import com.holaho.intern.integration.repository.IntegrationConnectionRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceIntegrationService {

    private final IntegrationConnectionRepository connectionRepository;
    private final AttendanceIntegrationEventRepository eventRepository;
    private final ExternalIdentityMappingRepository identityMappingRepository;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;

    @Transactional
    public AttendanceIntegrationEvent processEvent(Long connectionId, AttendanceEventDto eventDto) {
        Long tenantId = getTenantId();
        IntegrationConnection connection = connectionRepository.findById(connectionId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

        Optional<AttendanceIntegrationEvent> existingEvent = eventRepository
                .findByTenantIdAndConnectionIdAndExternalEventId(tenantId, connectionId, eventDto.getExternalEventId());

        if (existingEvent.isPresent()) {
            log.warn("Duplicate attendance event received: externalEventId={}", eventDto.getExternalEventId());
            AttendanceIntegrationEvent dupEvent = existingEvent.get();
            dupEvent.setProcessingStatus(EventProcessingStatus.DUPLICATE);
            return eventRepository.save(dupEvent);
        }

        AttendanceIntegrationEvent rawEvent = AttendanceIntegrationEvent.builder()
                .connection(connection)
                .externalEventId(eventDto.getExternalEventId())
                .externalEmployeeId(eventDto.getExternalEmployeeId())
                .eventType(eventDto.getEventType())
                .eventTime(eventDto.getEventTime())
                .deviceId(eventDto.getDeviceId())
                .method(eventDto.getMethod())
                .rawPayload(eventDto.getRawPayload())
                .processingStatus(EventProcessingStatus.UNPROCESSED)
                .build();
        rawEvent.setTenantId(tenantId);
        AttendanceIntegrationEvent savedEvent = eventRepository.save(rawEvent);

        try {
            InternProfile intern = resolveIntern(tenantId, connection.getCode(), eventDto.getExternalEmployeeId());
            if (intern == null) {
                savedEvent.setProcessingStatus(EventProcessingStatus.FAILED);
                savedEvent.setErrorMessage("EXTERNAL_EMPLOYEE_NOT_FOUND: Could not map " + eventDto.getExternalEmployeeId());
                return eventRepository.save(savedEvent);
            }

            LocalDate eventDate = eventDto.getEventTime().toLocalDate();
            Optional<Attendance> attendanceOpt = attendanceRepository.findByInternIdAndDate(intern.getId(), eventDate);

            Attendance attendance;
            if (attendanceOpt.isPresent()) {
                attendance = attendanceOpt.get();
            } else {
                attendance = Attendance.builder()
                        .intern(intern)
                        .date(eventDate)
                        .status("PRESENT")
                        .checkInMethod(eventDto.getMethod() != null ? eventDto.getMethod() : "QR")
                        .build();
                attendance.setTenantId(tenantId);
            }

            attendance.setSourceType(eventDto.getMethod() != null ? eventDto.getMethod() : "QR");
            attendance.setExternalEventId(eventDto.getExternalEventId());

            if ("CHECK_OUT".equalsIgnoreCase(eventDto.getEventType())) {
                attendance.setCheckOut(eventDto.getEventTime());
            } else {
                if (attendance.getCheckIn() == null) {
                    attendance.setCheckIn(eventDto.getEventTime());
                } else {
                    attendance.setCheckOut(eventDto.getEventTime());
                }
            }

            attendanceRepository.save(attendance);

            savedEvent.setProcessingStatus(EventProcessingStatus.PROCESSED);
            savedEvent.setProcessedAt(LocalDateTime.now());
            log.info("Successfully processed attendance event {} for intern {}", eventDto.getExternalEventId(), intern.getId());

        } catch (Exception e) {
            log.error("Failed to process attendance event {}: {}", eventDto.getExternalEventId(), e.getMessage(), e);
            savedEvent.setProcessingStatus(EventProcessingStatus.FAILED);
            savedEvent.setErrorMessage(e.getMessage());
        }

        return eventRepository.save(savedEvent);
    }

    private InternProfile resolveIntern(Long tenantId, String systemCode, String externalEmployeeId) {
        Optional<ExternalIdentityMapping> mappingOpt = identityMappingRepository
                .findByTenantIdAndSystemCodeAndEntityTypeAndExternalId(tenantId, systemCode, "EMPLOYEE", externalEmployeeId);

        if (mappingOpt.isPresent()) {
            Long userId = mappingOpt.get().getInternalId();
            return internProfileRepository.findByUserId(userId).orElse(null);
        }

        Optional<User> userOpt = userRepository.findByEmail(externalEmployeeId);
        if (userOpt.isPresent()) {
            return internProfileRepository.findByUserId(userOpt.get().getId()).orElse(null);
        }

        try {
            Long parsedId = Long.parseLong(externalEmployeeId);
            return internProfileRepository.findById(parsedId).orElse(null);
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
