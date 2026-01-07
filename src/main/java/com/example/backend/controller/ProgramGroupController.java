package com.example.backend.controller;

import com.example.backend.dto.request.AssignInternRequest;
import com.example.backend.dto.request.ProgramGroupCreateRequest;
import com.example.backend.dto.response.ProgramGroupResponse;
import com.example.backend.service.ProgramGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/program-groups")
@RequiredArgsConstructor
public class ProgramGroupController {

    private final ProgramGroupService programGroupService;

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping
    public ProgramGroupResponse create(@Valid @RequestBody ProgramGroupCreateRequest request) {
        return programGroupService.createGroup(request);
    }

    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    @PostMapping("/{groupId}/members")
    public void assignIntern(@PathVariable Long groupId, @Valid @RequestBody AssignInternRequest request) {
        programGroupService.assignIntern(groupId, request);
    }
}
