package com.holaho.intern.task.controller;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.task.entity.Task;
import com.holaho.intern.shared.security.CustomUserDetails;


import com.holaho.intern.shared.dto.request.TaskRequest;
import com.holaho.intern.shared.dto.request.TaskUpdateRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.shared.dto.response.TaskUpdateResponse;
import com.holaho.intern.task.entity.TaskUpdate;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.task.service.TaskService;
import com.holaho.intern.user.service.UserService;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
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
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAssignedToMe(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long userId = getUserIdFromAuth(authentication);
        Long internId = userService.getInternProfileIdByUserId(userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TaskResponse> tasks = taskService.getMyAssignedTasks(internId, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(tasks));
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
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getByGroup(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        String sortProp = "createdAt";
        Sort.Direction direction = Sort.Direction.DESC;

        if (sort != null && sort.length > 0 && sort[0] != null && !sort[0].isEmpty()) {
            if (sort.length > 1) {
                sortProp = sort[0];
                direction = "asc".equalsIgnoreCase(sort[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
            } else if (sort[0].contains(",")) {
                String[] parts = sort[0].split(",");
                sortProp = parts[0];
                if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1])) {
                    direction = Sort.Direction.ASC;
                }
            } else {
                sortProp = sort[0];
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProp));
        Page<TaskResponse> tasks = taskService.getTasksByGroup(groupId, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(tasks));
    }

    /**
     * Get tasks assigned to a specific intern (Mentor/HR)
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAssignedTasks(
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
        return ResponseEntity.ok(ApiResponse.successPage(tasks));
    }

    /**
     * Get all tasks (Mentor/HR/Admin/Intern)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = getUserIdFromAuth(authentication);

        boolean isIntern = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERN") || a.getAuthority().equals("INTERN"));
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MENTOR"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (isIntern && !isStaff) {
            Long internId = userService.getInternProfileIdByUserId(userId);
            Page<TaskResponse> tasks = taskService.getMyAssignedTasks(internId, pageable);
            return ResponseEntity.ok(ApiResponse.successPage(tasks));
        }

        Page<TaskResponse> tasks = taskService.getAllTasks(pageable);
        return ResponseEntity.ok(ApiResponse.successPage(tasks));
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
     * Update task progress (Intern)
     */
    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody TaskUpdateRequest request,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        Long internId = userService.getInternProfileIdByUserId(userId);

        com.holaho.intern.shared.dto.request.UpdateTaskProgressRequest req = new com.holaho.intern.shared.dto.request.UpdateTaskProgressRequest();
        req.setProgressPercent(request.getProgressPercent());
        req.setContent(request.getContent());

        TaskResponse res = taskService.updateTaskProgress(id, req, internId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tiến độ nhiệm vụ thành công", res));
    }

    /**
     * Get progress history timeline for a task
     */
    @GetMapping("/{id}/progress-history")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<com.holaho.intern.shared.dto.response.TaskProgressHistoryResponse>>> getProgressHistory(@PathVariable Long id) {
        List<com.holaho.intern.shared.dto.response.TaskProgressHistoryResponse> history = taskService.getProgressHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Intern submit completed task for mentor review
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<TaskResponse>> submitTask(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        TaskResponse res = taskService.submitTask(id, userId, note);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp báo cáo hoàn thành nhiệm vụ", res));
    }

    /**
     * Mentor approve submitted task
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> approveTask(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        TaskResponse res = taskService.approveTask(id, userId, note);
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt hoàn thành nhiệm vụ", res));
    }

    /**
     * Mentor reject submitted task / request changes
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> rejectTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        TaskResponse res = taskService.rejectTask(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã yêu cầu thực tập sinh làm lại nhiệm vụ", res));
    }

    /**
     * Mentor cancel task
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        TaskResponse res = taskService.cancelTask(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy nhiệm vụ", res));
    }

    /**
     * Get overdue tasks for mentor
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getOverdueTasks(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<TaskResponse> tasks = taskService.getOverdueTasksForMentor(userId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * Delete task
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully", null));
    }



    /**
     * Helper method to extract user ID from authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        Object principal = authentication.getPrincipal();

        // If using CustomUserDetails
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }

        // If using Spring's UserDetails, fetch from database by email
        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userService.getCurrentUser(email).getId();
        }

        throw new AuthenticationCredentialsNotFoundException("Unable to extract user ID from authentication");
    }
}

