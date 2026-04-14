package com.holaho.intern.controller;

import com.holaho.intern.entity.Mentor;
import com.holaho.intern.entity.Task;
import com.holaho.intern.shared.security.CustomUserDetails;


import com.holaho.intern.shared.dto.request.TaskRequest;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.shared.dto.response.TaskUpdateResponse;
import com.holaho.intern.entity.TaskUpdate;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.service.TaskService;
import com.holaho.intern.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserService userService;

    /**
     * Create new task (Mentor/HR only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        TaskResponse response = taskService.create(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", response));
    }

    /**
     * Update task (Mentor/HR only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);
        TaskResponse response = taskService.update(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    /**
     * Get tasks assigned to me (Intern)
     */
    @GetMapping("/assigned-to-me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getAssignedToMe(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getUserIdFromAuth(authentication);
        Long internId = userService.getInternProfileIdByUserId(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskResponse> tasks = taskService.getMyAssignedTasks(internId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Get task by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable Long id) {
        TaskResponse response = taskService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get tasks by group ID
     */
    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getByGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<TaskResponse> tasks = taskService.getTasksByGroup(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Get tasks assigned to a specific intern (Mentor/HR)
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getAssignedTasks(
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long creatorId = getUserIdFromAuth(authentication);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskResponse> tasks = taskService.getAssignedTasks(creatorId, assigneeId, groupId, status, keyword,
                pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Get all tasks (Mentor/HR)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TaskResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskResponse> tasks = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Update task status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {

        taskService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Task status updated successfully", null));
    }

    /**
     * Add progress update (Intern only)
     */
    @PostMapping("/{id}/updates")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Void>> addUpdate(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);

        // Get intern profile ID from user ID using UserService
        Long internId = userService.getInternProfileIdByUserId(userId);

        taskService.addUpdate(id, request, internId);
        return ResponseEntity.ok(ApiResponse.success("Task update added successfully", null));
    }

    /**
     * Get updates for a task
     */
    @GetMapping("/{id}/updates")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<TaskUpdateResponse>>> getUpdates(@PathVariable Long id) {
        List<TaskUpdate> updates = taskService.getTaskUpdates(id);
        List<TaskUpdateResponse> response = updates.stream()
                .map(TaskUpdateResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete task
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required");
        }

        Object principal = authentication.getPrincipal();

        // If using CustomUserDetails
        if (principal instanceof com.holaho.intern.shared.security.CustomUserDetails) {
            return ((com.holaho.intern.shared.security.CustomUserDetails) principal).getId();
        }

        // If using Spring's UserDetails, fetch from database by email
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userService.getCurrentUser(email).getId();
        }

        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}

