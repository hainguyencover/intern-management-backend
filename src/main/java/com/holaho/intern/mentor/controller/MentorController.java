package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.CreateMentorRequest;
import com.holaho.intern.mentor.dto.MentorStatusUpdateRequest;
import com.holaho.intern.mentor.dto.UpdateMentorRequest;
import com.holaho.intern.mentor.service.MentorService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.shared.dto.response.MentorDashboardStats;
import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> createMentor(@Valid @RequestBody CreateMentorRequest req) {
        MentorResponse created = mentorService.createMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mentor created successfully", created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN','MENTOR')")
    public ResponseEntity<ApiResponse<Page<MentorResponse>>> searchMentors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MentorStatus status,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(mentorService.searchMentors(search, status, departmentId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN','MENTOR')")
    public ResponseEntity<ApiResponse<MentorResponse>> getMentorById(@PathVariable Long id) {
        MentorResponse mentor = mentorService.getMentorById(id);
        return ResponseEntity.ok(ApiResponse.success(mentor));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> getMentorByUserId(@PathVariable Long userId) {
        MentorResponse mentor = mentorService.getMentorByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(mentor));
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<MentorDashboardStats>> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MentorDashboardStats stats = mentorService.getDashboardStats(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping({"/assigned-interns", "/assigned-to-me"})
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<InternProfileResponse>>> getAssignedInterns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<InternProfileResponse> page = mentorService.getAssignedInterns(
                userDetails.getEmail(), keyword, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/interns/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InternProfileResponse>> getInternDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(mentorService.getInternDetail(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> updateMentor(
            @PathVariable Long id, @Valid @RequestBody UpdateMentorRequest req) {
        MentorResponse updated = mentorService.updateMentor(id, req);
        return ResponseEntity.ok(ApiResponse.success("Mentor updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> updateMentorStatus(
            @PathVariable Long id, @Valid @RequestBody MentorStatusUpdateRequest req) {
        MentorResponse updated = mentorService.updateMentorStatus(id, req);
        return ResponseEntity.ok(ApiResponse.success("Mentor status updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMentor(@PathVariable Long id) {
        mentorService.deleteMentor(id);
        return ResponseEntity.ok(ApiResponse.success("Mentor deleted successfully", null));
    }
}
