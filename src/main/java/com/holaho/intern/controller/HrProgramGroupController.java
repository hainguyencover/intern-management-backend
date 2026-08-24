package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.AssignInternRequest;
import com.holaho.intern.shared.dto.request.GroupRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.GroupMemberResponse;
import com.holaho.intern.shared.dto.response.GroupResponse;
import com.holaho.intern.service.ProgramGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
@RequestMapping("/api/v1/hr")
public class HrProgramGroupController {

    private final ProgramGroupService programGroupService;

    @PostMapping("/programs/{programId}/groups")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(@PathVariable Long programId,
                                                     @Valid @RequestBody GroupRequest req) {
        req.setProgramId(programId);
        GroupResponse response = programGroupService.create(req);
        return ResponseEntity.ok(ApiResponse.success("Tạo nhóm thành công", response));
    }

    @GetMapping("/programs/{programId}/groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> listGroups(@PathVariable Long programId) {
        List<GroupResponse> response = programGroupService.getGroupsByProgramId(programId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<Void>> assignMember(@PathVariable Long groupId,
                                             @Valid @RequestBody AssignInternRequest req) {
        programGroupService.assignIntern(groupId, req.getInternId());
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên vào nhóm thành công", null));
    }

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> listMembers(@PathVariable Long groupId) {
        List<GroupMemberResponse> response = programGroupService.getMembers(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/groups/{groupId}/members/{internId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long groupId, @PathVariable Long internId) {
        programGroupService.removeIntern(groupId, internId);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành viên khỏi nhóm thành công", null));
    }
}


