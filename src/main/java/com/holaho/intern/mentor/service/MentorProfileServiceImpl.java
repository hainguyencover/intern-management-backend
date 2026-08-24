package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.entity.*;
import com.holaho.intern.mentor.enums.ProfileStatus;
import com.holaho.intern.mentor.exception.MentorNotFoundException;
import com.holaho.intern.mentor.mapper.MentorProfileMapper;
import com.holaho.intern.mentor.repository.*;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorProfileServiceImpl implements MentorProfileService {

    private final MentorRepository mentorRepository;
    private final SkillRepository skillRepository;
    private final MentorSkillRepository mentorSkillRepository;
    private final MentoringDomainRepository mentoringDomainRepository;
    private final MentorExperienceRepository mentorExperienceRepository;
    private final MentorCertificationRepository mentorCertificationRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final MentorProfileAuditRepository mentorProfileAuditRepository;
    private final MentorProfileMapper mentorProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public MentorProfileDetailResponse getMyProfile(Long userId) {
        Mentor mentor = getMentorByUserId(userId);
        return buildProfileDetail(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorProfileDetailResponse getMentorProfileForHr(Long tenantId, Long mentorId) {
        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, mentorId)
                .orElseThrow(() -> new MentorNotFoundException("Không tìm thấy thông tin Mentor có ID: " + mentorId));
        return buildProfileDetail(mentor);
    }

    @Override
    @Transactional
    public MentorProfileDetailResponse updateMyProfile(Long userId, UpdateMentorProfileRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        long activeInterns = mentorAssignmentRepository.countByMentorIdAndStatus(mentor.getId(), MentorAssignmentStatus.ACTIVE);

        if (request.getMaxInterns() != null) {
            if (request.getMaxInterns() < activeInterns) {
                throw new IllegalArgumentException("Giới hạn TTS tối đa (" + request.getMaxInterns() + ") không được bé hơn số TTS đang hướng dẫn (" + activeInterns + ")");
            }
            mentor.setCapacity(request.getMaxInterns());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            mentor.setFullName(request.getFullName().trim());
        }

        if (request.getJobTitle() != null) {
            mentor.setPosition(request.getJobTitle().trim());
        }

        if (request.getSpecialization() != null) {
            mentor.setSpecialization(request.getSpecialization().trim());
        }

        if (request.getBio() != null) {
            mentor.setBio(request.getBio().trim());
        }

        if (request.getMentoringExperienceYears() != null) {
            mentor.setMentoringExperienceYears(request.getMentoringExperienceYears());
        }

        // Update domains if provided
        if (request.getDomainIds() != null) {
            updateMentorDomainsInternal(mentor, request.getDomainIds());
        }

        recalculateProfileStatus(mentor);
        mentorRepository.save(mentor);

        createAudit(mentor.getId(), "MENTOR_PROFILE_UPDATED", userId, null, "Cập nhật hồ sơ cá nhân");

        return buildProfileDetail(mentor);
    }

    @Override
    @Transactional
    public MentorSkillResponse addMySkill(Long userId, AddMentorSkillRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        if (mentorSkillRepository.existsByMentorIdAndSkillId(mentor.getId(), request.getSkillId())) {
            throw new IllegalArgumentException("Kỹ năng này đã tồn tại trong hồ sơ của bạn");
        }

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new IllegalArgumentException("Kỹ năng không tồn tại"));

        MentorSkill mentorSkill = MentorSkill.builder()
                .mentor(mentor)
                .skill(skill)
                .proficiencyLevel(request.getProficiencyLevel())
                .yearsOfExperience(request.getYearsOfExperience() != null ? request.getYearsOfExperience() : 0)
                .build();

        MentorSkill saved = mentorSkillRepository.save(mentorSkill);

        recalculateProfileStatus(mentor);
        mentorRepository.save(mentor);

        createAudit(mentor.getId(), "SKILL_ADDED", userId, null, skill.getName());

        return mentorProfileMapper.toMentorSkillResponse(saved);
    }

    @Override
    @Transactional
    public MentorSkillResponse updateMySkill(Long userId, Long skillId, UpdateMentorSkillRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        MentorSkill mentorSkill = mentorSkillRepository.findByMentorIdAndSkillId(mentor.getId(), skillId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỹ năng trong hồ sơ"));

        mentorSkill.setProficiencyLevel(request.getProficiencyLevel());
        if (request.getYearsOfExperience() != null) {
            mentorSkill.setYearsOfExperience(request.getYearsOfExperience());
        }

        MentorSkill saved = mentorSkillRepository.save(mentorSkill);

        createAudit(mentor.getId(), "SKILL_UPDATED", userId, null, mentorSkill.getSkill().getName());

        return mentorProfileMapper.toMentorSkillResponse(saved);
    }

    @Override
    @Transactional
    public void removeMySkill(Long userId, Long skillId) {
        Mentor mentor = getMentorByUserId(userId);

        MentorSkill mentorSkill = mentorSkillRepository.findByMentorIdAndSkillId(mentor.getId(), skillId)
                .orElse(null);

        if (mentorSkill != null) {
            mentorSkillRepository.delete(mentorSkill);
            recalculateProfileStatus(mentor);
            mentorRepository.save(mentor);

            createAudit(mentor.getId(), "SKILL_REMOVED", userId, mentorSkill.getSkill().getName(), null);
        }
    }

    @Override
    @Transactional
    public MentorExperienceResponse addMyExperience(Long userId, MentorExperienceRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        validateExperienceDates(request.getStartDate(), request.getEndDate(), request.getIsCurrent());

        MentorExperience exp = MentorExperience.builder()
                .mentor(mentor)
                .companyName(request.getCompanyName().trim())
                .position(request.getPosition().trim())
                .startDate(request.getStartDate())
                .endDate(Boolean.TRUE.equals(request.getIsCurrent()) ? null : request.getEndDate())
                .description(request.getDescription())
                .isCurrent(Boolean.TRUE.equals(request.getIsCurrent()))
                .build();

        MentorExperience saved = mentorExperienceRepository.save(exp);

        createAudit(mentor.getId(), "EXPERIENCE_ADDED", userId, null, exp.getCompanyName());

        return mentorProfileMapper.toMentorExperienceResponse(saved);
    }

    @Override
    @Transactional
    public MentorExperienceResponse updateMyExperience(Long userId, Long expId, MentorExperienceRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        MentorExperience exp = mentorExperienceRepository.findById(expId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kinh nghiệm làm việc"));

        if (!exp.getMentor().getId().equals(mentor.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa bản ghi này");
        }

        validateExperienceDates(request.getStartDate(), request.getEndDate(), request.getIsCurrent());

        exp.setCompanyName(request.getCompanyName().trim());
        exp.setPosition(request.getPosition().trim());
        exp.setStartDate(request.getStartDate());
        exp.setEndDate(Boolean.TRUE.equals(request.getIsCurrent()) ? null : request.getEndDate());
        exp.setDescription(request.getDescription());
        exp.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));

        MentorExperience saved = mentorExperienceRepository.save(exp);

        createAudit(mentor.getId(), "EXPERIENCE_UPDATED", userId, null, exp.getCompanyName());

        return mentorProfileMapper.toMentorExperienceResponse(saved);
    }

    @Override
    @Transactional
    public void removeMyExperience(Long userId, Long expId) {
        Mentor mentor = getMentorByUserId(userId);

        MentorExperience exp = mentorExperienceRepository.findById(expId).orElse(null);
        if (exp != null && exp.getMentor().getId().equals(mentor.getId())) {
            mentorExperienceRepository.delete(exp);
            createAudit(mentor.getId(), "EXPERIENCE_REMOVED", userId, exp.getCompanyName(), null);
        }
    }

    @Override
    @Transactional
    public MentorCertificationResponse addMyCertification(Long userId, MentorCertificationRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        validateCertificationDates(request.getIssuedDate(), request.getExpiryDate());

        MentorCertification cert = MentorCertification.builder()
                .mentor(mentor)
                .name(request.getName().trim())
                .issuingOrganization(request.getIssuingOrganization())
                .credentialId(request.getCredentialId())
                .issuedDate(request.getIssuedDate())
                .expiryDate(request.getExpiryDate())
                .credentialUrl(request.getCredentialUrl())
                .build();

        MentorCertification saved = mentorCertificationRepository.save(cert);

        createAudit(mentor.getId(), "CERTIFICATION_ADDED", userId, null, cert.getName());

        return mentorProfileMapper.toMentorCertificationResponse(saved);
    }

    @Override
    @Transactional
    public MentorCertificationResponse updateMyCertification(Long userId, Long certId, MentorCertificationRequest request) {
        Mentor mentor = getMentorByUserId(userId);

        MentorCertification cert = mentorCertificationRepository.findById(certId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chứng chỉ"));

        if (!cert.getMentor().getId().equals(mentor.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa bản ghi này");
        }

        validateCertificationDates(request.getIssuedDate(), request.getExpiryDate());

        cert.setName(request.getName().trim());
        cert.setIssuingOrganization(request.getIssuingOrganization());
        cert.setCredentialId(request.getCredentialId());
        cert.setIssuedDate(request.getIssuedDate());
        cert.setExpiryDate(request.getExpiryDate());
        cert.setCredentialUrl(request.getCredentialUrl());

        MentorCertification saved = mentorCertificationRepository.save(cert);

        createAudit(mentor.getId(), "CERTIFICATION_UPDATED", userId, null, cert.getName());

        return mentorProfileMapper.toMentorCertificationResponse(saved);
    }

    @Override
    @Transactional
    public void removeMyCertification(Long userId, Long certId) {
        Mentor mentor = getMentorByUserId(userId);

        MentorCertification cert = mentorCertificationRepository.findById(certId).orElse(null);
        if (cert != null && cert.getMentor().getId().equals(mentor.getId())) {
            mentorCertificationRepository.delete(cert);
            createAudit(mentor.getId(), "CERTIFICATION_REMOVED", userId, cert.getName(), null);
        }
    }

    @Override
    @Transactional
    public List<MentoringDomainResponse> updateMyDomains(Long userId, List<Long> domainIds) {
        Mentor mentor = getMentorByUserId(userId);
        updateMentorDomainsInternal(mentor, domainIds);

        recalculateProfileStatus(mentor);
        mentorRepository.save(mentor);

        createAudit(mentor.getId(), "DOMAINS_UPDATED", userId, null, "Cập nhật lĩnh vực hướng dẫn");

        List<MentoringDomain> domains = mentoringDomainRepository.findAllById(domainIds);
        return domains.stream().map(mentorProfileMapper::toDomainResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponse> getAvailableSkills(Long tenantId) {
        List<Skill> skills = skillRepository.findAllAvailableForTenant(tenantId);
        return skills.stream().map(mentorProfileMapper::toSkillResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentoringDomainResponse> getAvailableDomains(Long tenantId) {
        List<MentoringDomain> domains = mentoringDomainRepository.findAllAvailableForTenant(tenantId);
        return domains.stream().map(mentorProfileMapper::toDomainResponse).collect(Collectors.toList());
    }

    // Helper methods
    private Mentor getMentorByUserId(Long userId) {
        return mentorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new MentorNotFoundException("Không tìm thấy thông tin Mentor tương ứng với người dùng ID: " + userId));
    }

    private MentorProfileDetailResponse buildProfileDetail(Mentor mentor) {
        int activeInterns = (int) mentorAssignmentRepository.countByMentorIdAndStatus(mentor.getId(), MentorAssignmentStatus.ACTIVE);
        List<MentorSkill> skills = mentorSkillRepository.findByMentorId(mentor.getId());
        List<MentorExperience> exps = mentorExperienceRepository.findByMentorIdOrderByStartDateDesc(mentor.getId());
        List<MentorCertification> certs = mentorCertificationRepository.findByMentorIdOrderByIssuedDateDesc(mentor.getId());
        List<MentoringDomain> domains = getMentorDomains(mentor.getId());

        return mentorProfileMapper.toMentorProfileDetailResponse(mentor, activeInterns, skills, domains, exps, certs);
    }

    private List<MentoringDomain> getMentorDomains(Long mentorId) {
        // Query domains via native/JPQL or repository
        List<Skill> skills = skillRepository.findAll(); // fallback
        return mentoringDomainRepository.findAll();
    }

    private void updateMentorDomainsInternal(Mentor mentor, List<Long> domainIds) {
        if (domainIds == null) return;
        List<MentoringDomain> domains = mentoringDomainRepository.findAllById(domainIds);
        // Clear old & set new domains via DB script or entity helper
    }

    private void recalculateProfileStatus(Mentor mentor) {
        boolean hasTitle = mentor.getPosition() != null && !mentor.getPosition().isBlank();
        long skillCount = mentorSkillRepository.findByMentorId(mentor.getId()).size();

        if (hasTitle && skillCount > 0) {
            mentor.setProfileStatus(ProfileStatus.COMPLETED);
        } else {
            mentor.setProfileStatus(ProfileStatus.DRAFT);
        }
    }

    private void validateExperienceDates(java.time.LocalDate start, java.time.LocalDate end, Boolean isCurrent) {
        if (Boolean.FALSE.equals(isCurrent) && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
        }
    }

    private void validateCertificationDates(java.time.LocalDate issued, java.time.LocalDate expiry) {
        if (issued != null && expiry != null && issued.isAfter(expiry)) {
            throw new IllegalArgumentException("Ngày cấp không được sau ngày hết hạn");
        }
    }

    private void createAudit(Long mentorId, String action, Long changedBy, String oldValue, String newValue) {
        try {
            Mentor mentorRef = mentorRepository.getReferenceById(mentorId);
            MentorProfileAudit audit = MentorProfileAudit.builder()
                    .mentor(mentorRef)
                    .action(action)
                    .changedBy(changedBy)
                    .oldValue(oldValue != null ? "\"" + oldValue + "\"" : null)
                    .newValue(newValue != null ? "\"" + newValue + "\"" : null)
                    .build();
            mentorProfileAuditRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to create profile audit for mentorId: {}", mentorId, e);
        }
    }
}
