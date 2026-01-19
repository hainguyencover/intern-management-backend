package com.example.backend.controller;

import com.example.backend.dto.TaskDto;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.InternTaskService;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interns/me")
public class InternTaskController {

    private final InternTaskService internTaskService;

    public InternTaskController(InternTaskService internTaskService) {
        this.internTaskService = internTaskService;
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/tasks")
    public Page<TaskDto> myTasks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return internTaskService.myTasks(principal.getId(), status, pageable);
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
