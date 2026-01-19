//package com.example.backend.controller;
//
//import com.example.backend.dto.TaskUpdateDto;
//import com.example.backend.dto.request.TaskUpdateRequest;
//import com.example.backend.security.CustomUserDetails;
//import com.example.backend.service.TaskService;
//import jakarta.validation.Valid;
//import org.springframework.data.domain.*;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/interns/me")
//public class InternTaskUpdateController {
//
//    private final TaskService taskUpdateService;
//
//    public InternTaskUpdateController(TaskService taskUpdateService) {
//        this.taskUpdateService = taskUpdateService;
//    }
//
//    @PreAuthorize("hasRole('INTERN')")
//    @PostMapping("/tasks/{taskId}/updates")
//    public TaskUpdateDto createUpdate(
//            @PathVariable Long taskId,
//            @Valid @RequestBody TaskUpdateRequest req,
//            @AuthenticationPrincipal CustomUserDetails principal
//    ) {
//        return taskUpdateService.internCreateUpdate(taskId, principal.getId(), req);
//    }
//
//    @PreAuthorize("hasRole('INTERN')")
//    @GetMapping("/tasks/{taskId}/updates")
//    public Page<TaskUpdateDto> myTaskUpdates(
//            @PathVariable Long taskId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(defaultValue = "id,desc") String sort,
//            @AuthenticationPrincipal CustomUserDetails principal
//    ) {
//        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
//        return taskUpdateService.internListMyTaskUpdates(taskId, principal.getId(), pageable);
//    }
//
//    private Sort parseSort(String sort) {
//        try {
//            String[] parts = sort.split(",");
//            String field = parts[0];
//            String dir = parts.length > 1 ? parts[1] : "desc";
//            return "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending() : Sort.by(field).descending();
//        } catch (Exception e) {
//            return Sort.by("id").descending();
//        }
//    }
//}
