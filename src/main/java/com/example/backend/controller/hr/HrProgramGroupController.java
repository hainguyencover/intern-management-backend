//package com.example.backend.controller.hr;
//
//import com.example.backend.dto.request.AssignMemberRequest;
//import com.example.backend.dto.request.CreateGroupRequest;
//import com.example.backend.dto.response.GroupResponse;
//import com.example.backend.dto.response.MemberResponse;
//import com.example.backend.service.ProgramGroupService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequiredArgsConstructor
//@PreAuthorize("hasRole('HR')")
//@RequestMapping("/api/hr")
//public class HrProgramGroupController {
//
//    private final ProgramGroupService programGroupService;
//
//    @PostMapping("/programs/{programId}/groups")
//    public ResponseEntity<GroupResponse> createGroup(@PathVariable Long programId,
//                                                     @Valid @RequestBody CreateGroupRequest req) {
//        return ResponseEntity.ok(programGroupService.createGroupHr(programId, req));
//    }
//
//    @GetMapping("/programs/{programId}/groups")
//    public ResponseEntity<List<GroupResponse>> listGroups(@PathVariable Long programId) {
//        return ResponseEntity.ok(programGroupService.listGroups(programId));
//    }
//
//    @PostMapping("/groups/{groupId}/members")
//    public ResponseEntity<Void> assignMember(@PathVariable Long groupId,
//                                             @Valid @RequestBody AssignMemberRequest req) {
//        // reuse assignIntern(req) trong service qua DTO cũ
//        var old = new com.example.backend.dto.request.AssignInternRequest();
//        old.setInternId(req.getInternId());
//        programGroupService.assignIntern(groupId, old);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/groups/{groupId}/members")
//    public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable Long groupId) {
//        return ResponseEntity.ok(programGroupService.listMembers(groupId));
//    }
//
//    @DeleteMapping("/groups/{groupId}/members/{internId}")
//    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @PathVariable Long internId) {
//        programGroupService.removeIntern(groupId, internId);
//        return ResponseEntity.noContent().build();
//    }
//}
