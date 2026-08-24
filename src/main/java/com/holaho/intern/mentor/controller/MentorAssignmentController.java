package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.service.MentorAssignmentService;
import com.holaho.intern.shared.dto.request.AssignMentorRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.AssignMentorResponse;
import com.holaho.intern.shared.dto.response.MentorAssignmentResponse;
import com.holaho.intern.shared.dto.response.MentorCapacityResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MentorAssignmentController {

    private final MentorAssignmentService mentorAssignmentService;

    @PostMapping("/mentor-assignments")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> createAssignment(
            @Valid @RequestBody com.holaho.intern.mentor.dto.AssignMentorRequest req) {
        MentorAssignmentResponse res = mentorAssignmentService.assignMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mentor assignment created successfully", res));
    }

    @PostMapping("/mentor-assignments/{assignmentId}/reassign")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> reassignMentor(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ReassignMentorRequest req) {
        MentorAssignmentResponse res = mentorAssignmentService.reassignMentor(assignmentId, req);
        return ResponseEntity.ok(ApiResponse.success("Mentor reassigned successfully", res));
    }

    @PatchMapping("/mentor-assignments/{assignmentId}/unassign")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> unassignMentor(
            @PathVariable Long assignmentId,
            @Valid @RequestBody UnassignMentorRequest req) {
        MentorAssignmentResponse res = mentorAssignmentService.unassignMentor(assignmentId, req);
        return ResponseEntity.ok(ApiResponse.success("Mentor unassigned successfully", res));
    }

    @PatchMapping("/mentor-assignments/{assignmentId}/complete")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> completeAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String reason) {
        MentorAssignmentResponse res = mentorAssignmentService.completeAssignment(assignmentId, reason);
        return ResponseEntity.ok(ApiResponse.success("Mentor assignment completed successfully", res));
    }

    @PatchMapping("/mentor-assignments/{assignmentId}/cancel")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorAssignmentResponse>> cancelAssignment(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String reason) {
        MentorAssignmentResponse res = mentorAssignmentService.cancelAssignment(assignmentId, reason);
        return ResponseEntity.ok(ApiResponse.success("Mentor assignment cancelled successfully", res));
    }

    @PostMapping("/mentor-assignments/bulk")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<BulkAssignMentorResponse>> bulkAssign(
            @Valid @RequestBody BulkAssignMentorRequest req) {
        BulkAssignMentorResponse res = mentorAssignmentService.bulkAssignMentors(req);
        return ResponseEntity.ok(ApiResponse.success("Bulk assignment processed", res));
    }

    @GetMapping("/mentor-assignments/suggest")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorSuggestionResponse>> suggestMentors(@RequestParam Long internId) {
        MentorSuggestionResponse res = mentorAssignmentService.suggestMentorsForIntern(internId);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/interns/{internId}/mentor-history")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MENTOR','INTERN')")
    public ResponseEntity<ApiResponse<List<MentorAssignmentResponse>>> getMentorHistory(@PathVariable Long internId) {
        List<MentorAssignmentResponse> history = mentorAssignmentService.getInternAssignmentHistory(internId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/mentors/{mentorId}/interns")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MENTOR')")
    public ResponseEntity<ApiResponse<List<MentorAssignmentResponse>>> getMentorInterns(@PathVariable Long mentorId) {
        List<MentorAssignmentResponse> list = mentorAssignmentService.getMentorActiveInterns(mentorId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/mentors/{mentorId}/capacity")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MENTOR')")
    public ResponseEntity<ApiResponse<MentorCapacityResponse>> getMentorCapacity(@PathVariable Long mentorId) {
        MentorCapacityResponse res = mentorAssignmentService.getMentorCapacity(mentorId);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    // Legacy fallback endpoints
    @PutMapping("/interns/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<AssignMentorResponse>> legacyAssignMentor(
            @PathVariable Long internId,
            @RequestBody AssignMentorRequest req) {
        AssignMentorResponse res = mentorAssignmentService.assignMentorToIntern(internId, req.getMentorId());
        return ResponseEntity.ok(ApiResponse.success("Phân công người hướng dẫn thành công", res));
    }

    @DeleteMapping("/interns/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<AssignMentorResponse>> legacyRemoveMentor(@PathVariable Long internId) {
        AssignMentorResponse res = mentorAssignmentService.removeMentorFromIntern(internId);
        return ResponseEntity.ok(ApiResponse.success("Gỡ bỏ người hướng dẫn thành công", res));
    }
}
