package com.holaho.intern.task.controller;

import com.holaho.intern.shared.dto.TaskDto;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.task.service.InternTaskService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/interns/me")
public class InternTaskController {

    private final InternTaskService internTaskService;
    private final com.holaho.intern.task.service.TaskService taskService;

    public InternTaskController(InternTaskService internTaskService, com.holaho.intern.task.service.TaskService taskService) {
        this.internTaskService = internTaskService;
        this.taskService = taskService;
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskDto>>> myTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity.ok(ApiResponse.successPage(internTaskService.myTasks(principal.getId(), status, pageable)));
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<com.holaho.intern.shared.dto.response.TaskResponse>> getTaskDetail(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(taskId)));
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

