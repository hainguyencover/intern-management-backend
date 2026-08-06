package com.holaho.intern.controller;

import com.holaho.intern.entity.Program;


import com.holaho.intern.shared.dto.request.AssignInternRequest;
import com.holaho.intern.shared.dto.request.GroupRequest;
import com.holaho.intern.shared.dto.request.CreateProgramRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.GroupResponse;
import com.holaho.intern.shared.dto.response.ProgramResponse;
import com.holaho.intern.shared.enums.ProgramStatus;
import com.holaho.intern.service.ProgramGroupService;
import com.holaho.intern.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final ProgramGroupService programGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR', 'INTERN')")
    public ResponseEntity<ApiResponse<List<ProgramResponse>>> search(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ProgramStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                sort[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sort[0]));

        Page<ProgramResponse> response = programService.search(departmentId, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR', 'INTERN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> getById(@PathVariable Long id) {
        ProgramResponse response = programService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> create(@Valid @RequestBody CreateProgramRequest req) {
        ProgramResponse response = programService.createProgram(req);
        return ResponseEntity.ok(ApiResponse.success("Program created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> update(@PathVariable Long id,
            @Valid @RequestBody CreateProgramRequest req) {
        ProgramResponse response = programService.updateProgram(id, req);
        return ResponseEntity.ok(ApiResponse.success("Program updated successfully", response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long id,
            @RequestParam ProgramStatus status) {
        programService.updateProgramStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Program status updated successfully", null));
    }

    // Group endpoints
    @PostMapping("/groups")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(@Valid @RequestBody GroupRequest req) {
        GroupResponse response = programGroupService.create(req);
        return ResponseEntity.ok(ApiResponse.success("Group created successfully", response));
    }

    @GetMapping("/{programId}/groups")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getGroups(@PathVariable Long programId) {
        List<GroupResponse> response = programGroupService.getGroupsByProgramId(programId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignIntern(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignInternRequest req) {
        programGroupService.assignIntern(groupId, req.getInternId());
        return ResponseEntity.ok(ApiResponse.success("Intern assigned to group successfully", null));
    }

    /**
     * Delete program
     * DELETE /api/v1/programs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        programService.deleteProgram(id);
        return ResponseEntity.ok(ApiResponse.success("Program deleted successfully", null));
    }
}

