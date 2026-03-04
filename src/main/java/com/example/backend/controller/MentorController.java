package com.example.backend.controller;

import com.example.backend.dto.request.CreateMentorRequest;
import com.example.backend.dto.response.*;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.MentorService;
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

    // POST /api/v1/mentors
    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> createMentor(@RequestBody CreateMentorRequest req) {
        MentorResponse created = mentorService.createMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mentor created successfully", created));
    }

    // GET /api/v1/mentors
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MentorResponse>>> getMentors(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(mentorService.getMentors(pageable)));
    }

    // GET /api/v1/mentors/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> getMentorById(@PathVariable Long id) {
        MentorResponse mentor = mentorService.getMentorById(id);
        return ResponseEntity.ok(ApiResponse.success(mentor));
    }

    // GET /api/v1/mentors/user/{userId}
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorResponse>> getMentorByUserId(@PathVariable Long userId) {
        MentorResponse mentor = mentorService.getMentorByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(mentor));
    }

    // GET /api/v1/mentors/me/dashboard
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<MentorDashboardStats>> getDashboardStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MentorDashboardStats stats = mentorService.getDashboardStats(userDetails.getEmail());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // GET /api/v1/mentors/assigned-interns
    @GetMapping("/assigned-interns")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<Page<InternProfileResponse>>> getAssignedInterns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<InternProfileResponse> page = mentorService.getAssignedInterns(
                userDetails.getEmail(), keyword, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    // GET /api/v1/mentors/interns/{id}
    @GetMapping("/interns/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InternProfileResponse>> getInternDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(mentorService.getInternDetail(id)));
    }
}
