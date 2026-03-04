package com.example.backend.controller;

import com.example.backend.dto.TaskUpdateDto;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.TaskService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mentor")
public class MentorTaskUpdateController {

    private final TaskService taskUpdateService;

    public MentorTaskUpdateController(TaskService taskUpdateService) {
        this.taskUpdateService = taskUpdateService;
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/tasks/{taskId}/updates")
    public ResponseEntity<ApiResponse<Page<TaskUpdateDto>>> taskUpdates(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return ResponseEntity
                .ok(ApiResponse.success(taskUpdateService.mentorListTaskUpdates(taskId, principal.getId(), pageable)));
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
