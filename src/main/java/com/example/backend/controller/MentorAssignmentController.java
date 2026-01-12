package com.example.backend.controller;

import com.example.backend.dto.request.AssignMentorRequest;
import com.example.backend.dto.response.AssignMentorResponse;
import com.example.backend.service.MentorAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interns")
public class MentorAssignmentController {

    private final MentorAssignmentService mentorAssignmentService;

    public MentorAssignmentController(MentorAssignmentService mentorAssignmentService) {
        this.mentorAssignmentService = mentorAssignmentService;
    }

    // HR gán mentor cho intern
    @PutMapping("/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<AssignMentorResponse> assignMentor(
            @PathVariable Long internId,
            @RequestBody AssignMentorRequest req
    ) {
        AssignMentorResponse res = mentorAssignmentService.assignMentorToIntern(internId, req.getMentorId());
        return ResponseEntity.ok(res);
    }

    // HR gỡ mentor khỏi intern
    @DeleteMapping("/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<AssignMentorResponse> removeMentor(@PathVariable Long internId) {
        AssignMentorResponse res = mentorAssignmentService.removeMentorFromIntern(internId);
        return ResponseEntity.ok(res);
    }
}
