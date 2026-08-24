package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.*;

import java.util.List;

public interface MentorProfileService {

    MentorProfileDetailResponse getMyProfile(Long userId);

    MentorProfileDetailResponse getMentorProfileForHr(Long tenantId, Long mentorId);

    MentorProfileDetailResponse updateMyProfile(Long userId, UpdateMentorProfileRequest request);

    MentorSkillResponse addMySkill(Long userId, AddMentorSkillRequest request);

    MentorSkillResponse updateMySkill(Long userId, Long skillId, UpdateMentorSkillRequest request);

    void removeMySkill(Long userId, Long skillId);

    MentorExperienceResponse addMyExperience(Long userId, MentorExperienceRequest request);

    MentorExperienceResponse updateMyExperience(Long userId, Long expId, MentorExperienceRequest request);

    void removeMyExperience(Long userId, Long expId);

    MentorCertificationResponse addMyCertification(Long userId, MentorCertificationRequest request);

    MentorCertificationResponse updateMyCertification(Long userId, Long certId, MentorCertificationRequest request);

    void removeMyCertification(Long userId, Long certId);

    List<MentoringDomainResponse> updateMyDomains(Long userId, List<Long> domainIds);

    List<SkillResponse> getAvailableSkills(Long tenantId);

    List<MentoringDomainResponse> getAvailableDomains(Long tenantId);
}
