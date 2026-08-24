package com.holaho.intern.integration.controller;

import com.holaho.intern.integration.adapter.AttendanceEventDto;
import com.holaho.intern.integration.dto.AttendanceWebhookRequest;
import com.holaho.intern.integration.entity.AttendanceIntegrationEvent;
import com.holaho.intern.integration.service.AttendanceIntegrationService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/attendance")
@RequiredArgsConstructor
public class AttendanceIntegrationController {

    private final AttendanceIntegrationService attendanceIntegrationService;

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleAttendanceWebhook(
            @RequestParam(required = false, defaultValue = "1") Long connectionId,
            @RequestBody AttendanceWebhookRequest request
    ) {
        AttendanceEventDto eventDto = AttendanceEventDto.builder()
                .externalEventId(request.getEventId())
                .externalEmployeeId(request.getEmployeeId())
                .eventType(request.getEventType())
                .eventTime(request.getEventTime())
                .deviceId(request.getDeviceId())
                .method(request.getMethod() != null ? request.getMethod() : "QR")
                .rawPayload(request.getRawPayload())
                .build();

        AttendanceIntegrationEvent processedEvent = attendanceIntegrationService.processEvent(connectionId, eventDto);
        return ResponseEntity.ok(ApiResponse.success(
                "Tiếp nhận sự kiện chấm công thành công",
                processedEvent.getProcessingStatus().name()
        ));
    }
}
