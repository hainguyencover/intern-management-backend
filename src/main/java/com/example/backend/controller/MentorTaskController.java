package com.example.backend.controller;

import com.example.backend.dto.GroupInternDto;
import com.example.backend.dto.TaskDto;
import com.example.backend.dto.request.CreateMentorTaskRequest;
import com.example.backend.dto.response.CreateMentorTaskResponse;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.MentorTaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mentor")
public class MentorTaskController {

    private final MentorTaskService mentorTaskService;

    public MentorTaskController(MentorTaskService mentorTaskService) {
        this.mentorTaskService = mentorTaskService;
    }

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/tasks")
    public CreateMentorTaskResponse createTasks(
            @Valid @RequestBody CreateMentorTaskRequest req,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return mentorTaskService.createTasks(req, principal.getId());
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/tasks")
    public Page<TaskDto> listTasks(
            @RequestParam Long groupId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return mentorTaskService.listTasks(groupId, status, pageable, principal.getId());
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/groups/{groupId}/interns")
    public List<GroupInternDto> internsInGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return mentorTaskService.listInternsInGroup(groupId, principal.getId());
    }

    private Sort parseSort(String sort) {
        // format: "field,asc" or "field,desc"
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
