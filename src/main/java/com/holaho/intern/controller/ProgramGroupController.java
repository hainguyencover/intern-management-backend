package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.AssignInternRequest;
import com.holaho.intern.shared.dto.request.AssignInternsRequest;
import com.holaho.intern.shared.dto.request.GroupRequest;
import com.holaho.intern.shared.dto.request.UpdateGroupRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.GroupMemberResponse;
import com.holaho.intern.shared.dto.response.GroupResponse;
import com.holaho.intern.service.ProgramGroupService;
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
@RequestMapping("/api/v1/program-groups")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class ProgramGroupController {

    private final ProgramGroupService groupService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<GroupResponse>>> getAll(
            @RequestParam(required = false) Long programId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<GroupResponse> groups = groupService.search(programId, status, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getById(@PathVariable Long id) {
        GroupResponse group = groupService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(group));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GroupResponse>> create(@Valid @RequestBody GroupRequest request) {
        GroupResponse group = groupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Group created successfully", group));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupRequest request) {
        GroupResponse group = groupService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Group updated successfully", group));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Group deleted successfully", null));
    }

    // ========== MEMBER MANAGEMENT ==========

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getMembers(@PathVariable Long id) {
        List<GroupMemberResponse> members = groupService.getMembers(id);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> assignIntern(
            @PathVariable Long id,
            @Valid @RequestBody AssignInternRequest request) {
        groupService.assignIntern(id, request.getInternId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Intern assigned to group successfully", null));
    }

    @PostMapping("/{id}/members/bulk")
    public ResponseEntity<ApiResponse<Void>> assignInterns(
            @PathVariable Long id,
            @Valid @RequestBody AssignInternsRequest request) {
        groupService.assignInterns(id, request.getInternIds());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Interns assigned to group successfully", null));
    }

    @DeleteMapping("/{groupId}/members/{internId}")
    public ResponseEntity<ApiResponse<Void>> removeIntern(
            @PathVariable Long groupId,
            @PathVariable Long internId) {
        groupService.removeIntern(groupId, internId);
        return ResponseEntity.ok(ApiResponse.success("Intern removed from group successfully", null));
    }
}

