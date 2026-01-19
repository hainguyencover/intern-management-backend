package com.example.backend.controller;

import com.example.backend.dto.request.CreateMentorRequest;
import com.example.backend.dto.response.MentorResponse;
import com.example.backend.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    // POST /api/mentors
    @PostMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<MentorResponse> createMentor(@RequestBody CreateMentorRequest req) {
        MentorResponse created = mentorService.createMentor(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/mentors
    @GetMapping
    public ResponseEntity<Page<MentorResponse>> getMentors(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(mentorService.getMentors(pageable));
    }

    // GET /api/mentors/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<MentorResponse> getMentorById(@PathVariable Long id) {
        MentorResponse mentor = mentorService.getMentorById(id);
        return ResponseEntity.ok(mentor);
    }

    // GET /api/mentors/me/dashboard
    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<com.example.backend.dto.response.MentorDashboardStats> getDashboardStats(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.backend.security.CustomUserDetails userDetails) {
        com.example.backend.dto.response.MentorDashboardStats stats = mentorService
                .getDashboardStats(userDetails.getEmail());
        return ResponseEntity.ok(stats);
    }

    // GET /api/mentors/assigned-interns
    @GetMapping("/assigned-interns")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<Page<com.example.backend.dto.response.InternProfileResponse>> getAssignedInterns(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.backend.security.CustomUserDetails userDetails) {
        Page<com.example.backend.dto.response.InternProfileResponse> page = mentorService.getAssignedInterns(
                userDetails.getEmail(), keyword, status, pageable);
        return ResponseEntity.ok(page);
    }

    // GET /api/mentors/interns/{id}
    @GetMapping("/interns/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<com.example.backend.dto.response.InternProfileResponse> getInternDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(mentorService.getInternDetail(id));
    }
}
