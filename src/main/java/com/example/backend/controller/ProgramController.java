package com.example.backend.controller;

import com.example.backend.dto.request.AssignInternRequest;
import com.example.backend.dto.request.GroupRequest;
import com.example.backend.dto.request.CreateProgramRequest;
import com.example.backend.dto.response.GroupResponse;
import com.example.backend.dto.response.ProgramResponse;
import com.example.backend.enums.ProgramStatus;
import com.example.backend.service.ProgramGroupService;
import com.example.backend.service.ProgramService;
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
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final ProgramGroupService programGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<Page<ProgramResponse>> search(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ProgramStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(
                sort[1].equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sort[0]));

        return ResponseEntity.ok(programService.search(departmentId, status, keyword, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR', 'INTERN')")
    public ResponseEntity<ProgramResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ProgramResponse> create(@Valid @RequestBody CreateProgramRequest req) {
        return ResponseEntity.ok(programService.createProgram(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ProgramResponse> update(@PathVariable Long id, @Valid @RequestBody CreateProgramRequest req) {
        return ResponseEntity.ok(programService.updateProgram(id, req));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestParam ProgramStatus status) {
        programService.updateProgramStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // Group endpoints
    @PostMapping("/groups")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest req) {
        return ResponseEntity.ok(programGroupService.create(req));
    }

    @GetMapping("/{programId}/groups")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<List<GroupResponse>> getGroups(@PathVariable Long programId) {
        return ResponseEntity.ok(programGroupService.getGroupsByProgramId(programId));
    }

    @PostMapping("/groups/{groupId}/members")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> assignIntern(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignInternRequest req) {
        programGroupService.assignIntern(groupId, req.getInternId());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete program
     * DELETE /api/programs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
}
