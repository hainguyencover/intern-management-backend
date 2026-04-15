package com.holaho.intern.task.controller;

import com.holaho.intern.shared.dto.TaskUpdateDto;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interns/me")
public class InternTaskUpdateController {

    private final TaskService taskUpdateService;

    public InternTaskUpdateController(TaskService taskUpdateService) {
        this.taskUpdateService = taskUpdateService;
    }

    @PreAuthorize("hasRole('INTERN')")
    @PostMapping("/tasks/{taskId}/updates")
    public ResponseEntity<ApiResponse<TaskUpdateDto>> createUpdate(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Báo cáo tiến độ công việc thành công",
                taskUpdateService.internCreateUpdate(taskId, principal.getId(), req)));
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/tasks/{taskId}/updates")
    public ResponseEntity<ApiResponse<Page<TaskUpdateDto>>> myTaskUpdates(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity.ok(
                ApiResponse.success(taskUpdateService.internListMyTaskUpdates(taskId, principal.getId(), pageable)));
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0];
            String dir = parts.length > 1 ? parts[1] : "desc";
            return "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending() : Sort.by(field).descending();
        } catch (Exception e) {
            return Sort.by("id").descending();
        }
    }
}
