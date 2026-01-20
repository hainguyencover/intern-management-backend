package com.example.backend.controller;

import com.example.backend.dto.InternSearchCriteria;
import com.example.backend.dto.request.InternProfileRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.service.InternProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Intern Profile Controller - CRUD Intern Profiles
 * Endpoints: /api/interns/**
 */
@RestController
@RequestMapping("/api/interns")
@RequiredArgsConstructor
@Slf4j
public class InternController {

    private final InternProfileService internProfileService;

    /**
     * Search/Filter interns (HR/Admin/Mentor)
     * GET /api/interns/search?university=&major=&keyword=&status=&page=0&size=10
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<Page<InternProfileResponse>> searchInterns(
            @RequestParam(required = false) String university,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean excludeBusy,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Search interns - university: {}, major: {}, keyword: {}", university, major, keyword);
        InternSearchCriteria criteria = InternSearchCriteria.builder()
                .university(university)
                .major(major)
                .keyword(keyword)
                .keyword(keyword)
                .status(status)
                .excludeBusy(excludeBusy)
                .build();
        Page<InternProfileResponse> interns = internProfileService.searchInterns(criteria, pageable);
        return ResponseEntity.ok(interns);
    }

    /**
     * Get intern by ID
     * GET /api/interns/profiles/{id}
     */
    @GetMapping("/profiles/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR', 'INTERN')")
    public ResponseEntity<InternProfileResponse> getInternById(@PathVariable Long id) {
        log.info("Get intern profile: {}", id);
        InternProfileResponse intern = internProfileService.getInternProfileById(id);
        return ResponseEntity.ok(intern);
    }

    /**
     * Create intern profile (HR/Admin)
     * POST /api/interns/profiles
     */
    @PostMapping("/profiles")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<InternProfileResponse> createIntern(
            @Valid @RequestBody InternProfileRequest request) {
        log.info("Create intern profile for user: {}", request.getUserId());
        InternProfileResponse intern = internProfileService.createIntern(request);
        return ResponseEntity.ok(intern);
    }

    /**
     * Update intern profile (HR/Admin or own profile)
     * PUT /api/interns/profiles/{id}
     */
    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<InternProfileResponse> updateIntern(
            @PathVariable Long id,
            @Valid @RequestBody InternProfileRequest request) {
        log.info("Update intern profile: {}", id);
        InternProfileResponse intern = internProfileService.updateIntern(id, request);
        return ResponseEntity.ok(intern);
    }

    /**
     * Delete intern profile (HR/Admin only)
     * DELETE /api/interns/profiles/{id}
     */
    @DeleteMapping("/profiles/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> deleteIntern(@PathVariable Long id) {
        log.info("Delete intern profile: {}", id);
        internProfileService.deleteIntern(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get my profile (Intern)
     * GET /api/interns/me
     */
    @GetMapping({ "/me", "/me/profile" })
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<InternProfileResponse> getMyProfile(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.backend.security.CustomUserDetails userDetails) {
        log.info("Get my intern profile");
        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }
        InternProfileResponse profile = internProfileService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update my profile (Intern)
     * PUT /api/interns/me/profile
     */
    @PutMapping({ "/me", "/me/profile" })
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<InternProfileResponse> updateMyProfile(
            @Valid @RequestBody InternProfileRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.backend.security.CustomUserDetails userDetails) {
        log.info("Update my intern profile");
        InternProfileResponse profile = internProfileService.updateMyProfile(request, userDetails.getEmail());
        return ResponseEntity.ok(profile);
    }

    /**
     * Assign mentor to intern (HR/Admin)
     * PUT /api/interns/profiles/{id}/assign-mentor?mentorUserId=...
     */
    @PutMapping("/profiles/{id}/assign-mentor")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Void> assignMentor(
            @PathVariable Long id,
            @RequestParam(required = false) Long mentorUserId) {
        log.info("Assign mentor (User ID: {}) to intern {}", mentorUserId, id);
        internProfileService.assignMentorByUserId(id, mentorUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Stats: Count by University
     * GET /api/interns/stats/university
     */
    @GetMapping("/stats/university")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<java.util.List<com.example.backend.dto.InternCountStatDto>> getStatsUniversity() {
        return ResponseEntity.ok(internProfileService.getInternStatsByUniversity());
    }

    /**
     * Stats: Count by Major
     * GET /api/interns/stats/major
     */
    @GetMapping("/stats/major")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<java.util.List<com.example.backend.dto.InternCountStatDto>> getStatsMajor() {
        return ResponseEntity.ok(internProfileService.getInternStatsByMajor());
    }
}
