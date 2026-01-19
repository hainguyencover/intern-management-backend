package com.example.backend.controller;

import com.example.backend.dto.request.AssignInternRequest;
import com.example.backend.dto.request.AssignInternsRequest;
import com.example.backend.dto.request.GroupRequest;
import com.example.backend.dto.request.UpdateGroupRequest;
import com.example.backend.dto.response.GroupMemberResponse;
import com.example.backend.dto.response.GroupResponse;
import com.example.backend.service.ProgramGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class ProgramGroupController {

    private final ProgramGroupService groupService;

    /**
     * Get groups with filtering and pagination
     * GET /api/program-groups?programId=1&status=ACTIVE&q=marketing&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getAll(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<GroupResponse> groups = groupService.search(programId, status, q, pageable);
        return ResponseEntity.ok(groups);
    }

    /**
     * Get group by ID
     * GET /api/program-groups/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getById(@PathVariable Long id) {
        GroupResponse group = groupService.getById(id);
        return ResponseEntity.ok(group);
    }

    /**
     * Create new group
     * POST /api/program-groups
     */
    @PostMapping
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
        GroupResponse group = groupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /**
     * Update group
     * PUT /api/program-groups/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupResponse group = groupService.update(id, request);
        return ResponseEntity.ok(group);
    }

    /**
     * Delete group
     * DELETE /api/program-groups/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== MEMBER MANAGEMENT ==========

    /**
     * Get members of a group
     * GET /api/program-groups/{id}/members
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(@PathVariable Long id) {
        List<GroupMemberResponse> members = groupService.getMembers(id);
        return ResponseEntity.ok(members);
    }

    /**
     * Assign single intern to group
     * POST /api/program-groups/{id}/members
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> assignIntern(
            @PathVariable Long id,
            @Valid @RequestBody AssignInternRequest request) {
        groupService.assignIntern(id, request.getInternId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Assign multiple interns to group
     * POST /api/program-groups/{id}/members/bulk
     */
    @PostMapping("/{id}/members/bulk")
    public ResponseEntity<Void> assignInterns(
            @PathVariable Long id,
            @Valid @RequestBody AssignInternsRequest request) {
        groupService.assignInterns(id, request.getInternIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Remove intern from group
     * DELETE /api/program-groups/{groupId}/members/{internId}
     */
    @DeleteMapping("/{groupId}/members/{internId}")
    public ResponseEntity<Void> removeIntern(
            @PathVariable Long groupId,
            @PathVariable Long internId) {
        groupService.removeIntern(groupId, internId);
        return ResponseEntity.noContent().build();
    }
}
