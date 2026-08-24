package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.service.MentorProfileService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mentors/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MENTOR')")
public class MentorProfileController {

    private final MentorProfileService mentorProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MentorProfileDetailResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MentorProfileDetailResponse response = mentorProfileService.getMyProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<MentorProfileDetailResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateMentorProfileRequest request) {
        MentorProfileDetailResponse response = mentorProfileService.updateMyProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", response));
    }

    // Skills
    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<MentorSkillResponse>> addMySkill(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddMentorSkillRequest request) {
        MentorSkillResponse response = mentorProfileService.addMySkill(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm kỹ năng thành công", response));
    }

    @PatchMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<MentorSkillResponse>> updateMySkill(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long skillId,
            @Valid @RequestBody UpdateMentorSkillRequest request) {
        MentorSkillResponse response = mentorProfileService.updateMySkill(userDetails.getId(), skillId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kỹ năng thành công", response));
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<ApiResponse<Void>> removeMySkill(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long skillId) {
        mentorProfileService.removeMySkill(userDetails.getId(), skillId);
        return ResponseEntity.ok(ApiResponse.success("Xóa kỹ năng thành công", null));
    }

    // Experiences
    @PostMapping("/experiences")
    public ResponseEntity<ApiResponse<MentorExperienceResponse>> addMyExperience(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MentorExperienceRequest request) {
        MentorExperienceResponse response = mentorProfileService.addMyExperience(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm kinh nghiệm làm việc thành công", response));
    }

    @PatchMapping("/experiences/{expId}")
    public ResponseEntity<ApiResponse<MentorExperienceResponse>> updateMyExperience(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long expId,
            @Valid @RequestBody MentorExperienceRequest request) {
        MentorExperienceResponse response = mentorProfileService.updateMyExperience(userDetails.getId(), expId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kinh nghiệm thành công", response));
    }

    @DeleteMapping("/experiences/{expId}")
    public ResponseEntity<ApiResponse<Void>> removeMyExperience(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long expId) {
        mentorProfileService.removeMyExperience(userDetails.getId(), expId);
        return ResponseEntity.ok(ApiResponse.success("Xóa kinh nghiệm thành công", null));
    }

    // Certifications
    @PostMapping("/certifications")
    public ResponseEntity<ApiResponse<MentorCertificationResponse>> addMyCertification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody MentorCertificationRequest request) {
        MentorCertificationResponse response = mentorProfileService.addMyCertification(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Thêm chứng chỉ thành công", response));
    }

    @PatchMapping("/certifications/{certId}")
    public ResponseEntity<ApiResponse<MentorCertificationResponse>> updateMyCertification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long certId,
            @Valid @RequestBody MentorCertificationRequest request) {
        MentorCertificationResponse response = mentorProfileService.updateMyCertification(userDetails.getId(), certId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chứng chỉ thành công", response));
    }

    @DeleteMapping("/certifications/{certId}")
    public ResponseEntity<ApiResponse<Void>> removeMyCertification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long certId) {
        mentorProfileService.removeMyCertification(userDetails.getId(), certId);
        return ResponseEntity.ok(ApiResponse.success("Xóa chứng chỉ thành công", null));
    }

    // Mentoring Domains
    @PutMapping("/domains")
    public ResponseEntity<ApiResponse<List<MentoringDomainResponse>>> updateMyDomains(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody List<Long> domainIds) {
        List<MentoringDomainResponse> response = mentorProfileService.updateMyDomains(userDetails.getId(), domainIds);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lĩnh vực hướng dẫn thành công", response));
    }

    // Catalog queries
    @GetMapping("/available-skills")
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getAvailableSkills() {
        return ResponseEntity.ok(ApiResponse.success(mentorProfileService.getAvailableSkills(null)));
    }

    @GetMapping("/available-domains")
    public ResponseEntity<ApiResponse<List<MentoringDomainResponse>>> getAvailableDomains() {
        return ResponseEntity.ok(ApiResponse.success(mentorProfileService.getAvailableDomains(null)));
    }
}
