package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.service.MentorMatchingService;
import com.holaho.intern.mentor.service.MentorProfileService;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/mentors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class HrMentorProfileController {

    private final MentorProfileService mentorProfileService;
    private final MentorMatchingService mentorMatchingService;

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    @GetMapping("/{mentorId}/profile")
    public ResponseEntity<ApiResponse<MentorProfileDetailResponse>> getMentorProfileForHr(
            @PathVariable Long mentorId) {
        MentorProfileDetailResponse profile = mentorProfileService.getMentorProfileForHr(getTenantId(), mentorId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<List<MentorMatchResultResponse>>> matchMentors(
            @RequestBody MentorMatchFilterRequest filter) {
        List<MentorMatchResultResponse> matches = mentorMatchingService.matchMentorsForHr(getTenantId(), filter);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm & matching thành công", matches));
    }
}
