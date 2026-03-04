package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.request.QrLogDto;
import com.example.backend.service.AttendanceService;
import com.example.backend.service.HrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final HrmService hrmService;
    private final AttendanceService attendanceService;

    /**
     * Trigger HRM Synchronization
     * POST /api/v1/admin/integrations/hrm/sync
     */
    @PostMapping("/hrm/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> syncHrm() {
        String result = hrmService.syncData();
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ HRM thành công", result));
    }

    /**
     * Receive QR/Card Logs from Device
     * POST /api/v1/admin/integrations/timekeeping/sync
     */
    @PostMapping("/timekeeping/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> syncTimekeeping(@RequestBody List<QrLogDto> logs) {
        String result = attendanceService.syncQrData(logs);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ dữ liệu chấm công thành công", result));
    }
}
