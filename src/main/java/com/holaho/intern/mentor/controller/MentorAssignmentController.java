package com.holaho.intern.mentor.controller;

import com.holaho.intern.shared.dto.request.AssignMentorRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.AssignMentorResponse;
import com.holaho.intern.mentor.service.MentorAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interns")
public class MentorAssignmentController {

    private final MentorAssignmentService mentorAssignmentService;

    public MentorAssignmentController(MentorAssignmentService mentorAssignmentService) {
        this.mentorAssignmentService = mentorAssignmentService;
    }

    @PutMapping("/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<AssignMentorResponse>> assignMentor(
            @PathVariable Long internId,
            @RequestBody AssignMentorRequest req) {
        AssignMentorResponse res = mentorAssignmentService.assignMentorToIntern(internId, req.getMentorId());
        return ResponseEntity.ok(ApiResponse.success("PhÃ¢n cÃ´ng ngÆ°á»i hÆ°á»›ng dáº«n thÃ nh cÃ´ng", res));
    }

    @DeleteMapping("/{internId}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<AssignMentorResponse>> removeMentor(@PathVariable Long internId) {
        AssignMentorResponse res = mentorAssignmentService.removeMentorFromIntern(internId);
        return ResponseEntity.ok(ApiResponse.success("Gá»¡ bá» ngÆ°á»i hÆ°á»›ng dáº«n thÃ nh cÃ´ng", res));
    }
}

