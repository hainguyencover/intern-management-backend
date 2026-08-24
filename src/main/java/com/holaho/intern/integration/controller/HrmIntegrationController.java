package com.holaho.intern.integration.controller;

import com.holaho.intern.integration.dto.SyncJobResponse;
import com.holaho.intern.integration.entity.IntegrationSyncJob;
import com.holaho.intern.integration.repository.IntegrationSyncJobRepository;
import com.holaho.intern.integration.service.HrmSyncService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/integrations/hrm")
@RequiredArgsConstructor
public class HrmIntegrationController {

    private final HrmSyncService hrmSyncService;
    private final IntegrationSyncJobRepository syncJobRepository;

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncJobResponse>> syncHrm(@RequestBody Map<String, Long> payload) {
        Long connectionId = payload.get("connectionId");
        if (connectionId == null) {
            throw new IllegalArgumentException("connectionId is required");
        }

        IntegrationSyncJob job = hrmSyncService.startHrmSync(connectionId);
        hrmSyncService.executeHrmSyncAsync(job.getId());

        SyncJobResponse response = mapToResponse(job);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Khởi tạo tiến trình đồng bộ HRM thành công", response));
    }

    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SyncJobResponse>> getSyncJobStatus(@PathVariable Long jobId) {
        IntegrationSyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Sync job not found: " + jobId));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(job)));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SyncJobResponse>>> getSyncJobHistory(@RequestParam Long connectionId) {
        List<SyncJobResponse> history = syncJobRepository.findByConnectionIdOrderByCreatedAtDesc(connectionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    private SyncJobResponse mapToResponse(IntegrationSyncJob job) {
        return SyncJobResponse.builder()
                .id(job.getId())
                .connectionId(job.getConnection().getId())
                .connectionCode(job.getConnection().getCode())
                .syncType(job.getSyncType())
                .direction(job.getDirection())
                .status(job.getStatus())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .totalRecords(job.getTotalRecords())
                .successRecords(job.getSuccessRecords())
                .failedRecords(job.getFailedRecords())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
