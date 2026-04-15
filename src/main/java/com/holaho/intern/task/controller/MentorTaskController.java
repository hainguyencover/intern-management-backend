package com.holaho.intern.task.controller;

import com.holaho.intern.shared.dto.GroupInternDto;
import com.holaho.intern.shared.dto.TaskDto;
import com.holaho.intern.shared.dto.request.CreateMentorTaskRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.CreateMentorTaskResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.task.service.MentorTaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mentor")
public class MentorTaskController {

    private final MentorTaskService mentorTaskService;

    public MentorTaskController(MentorTaskService mentorTaskService) {
        this.mentorTaskService = mentorTaskService;
    }

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<CreateMentorTaskResponse>> createTasks(
            @Valid @RequestBody CreateMentorTaskRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Giao nhiệm vụ thành công",
                mentorTaskService.createTasks(req, principal.getId())));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Page<TaskDto>>> listTasks(
            @RequestParam Long groupId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return ResponseEntity
                .ok(ApiResponse.success(mentorTaskService.listTasks(groupId, status, pageable, principal.getId())));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/groups/{groupId}/interns")
    public ResponseEntity<ApiResponse<List<GroupInternDto>>> internsInGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(mentorTaskService.listInternsInGroup(groupId, principal.getId())));
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
