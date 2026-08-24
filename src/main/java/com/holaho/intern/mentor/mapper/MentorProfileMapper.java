package com.holaho.intern.mentor.mapper;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MentorProfileMapper {

    public SkillResponse toSkillResponse(Skill skill) {
        if (skill == null) return null;
        return SkillResponse.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .isActive(skill.getIsActive())
                .build();
    }

    public MentoringDomainResponse toDomainResponse(MentoringDomain domain) {
        if (domain == null) return null;
        return MentoringDomainResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .isActive(domain.getIsActive())
                .build();
    }

    public MentorSkillResponse toMentorSkillResponse(MentorSkill mentorSkill) {
        if (mentorSkill == null) return null;
        return MentorSkillResponse.builder()
                .id(mentorSkill.getId())
                .skillId(mentorSkill.getSkill() != null ? mentorSkill.getSkill().getId() : null)
                .name(mentorSkill.getSkill() != null ? mentorSkill.getSkill().getName() : null)
                .category(mentorSkill.getSkill() != null ? mentorSkill.getSkill().getCategory() : null)
                .proficiencyLevel(mentorSkill.getProficiencyLevel())
                .yearsOfExperience(mentorSkill.getYearsOfExperience())
                .build();
    }

    public MentorExperienceResponse toMentorExperienceResponse(MentorExperience experience) {
        if (experience == null) return null;
        return MentorExperienceResponse.builder()
                .id(experience.getId())
                .companyName(experience.getCompanyName())
                .position(experience.getPosition())
                .startDate(experience.getStartDate())
                .endDate(experience.getEndDate())
                .description(experience.getDescription())
                .isCurrent(experience.getIsCurrent())
                .build();
    }

    public MentorCertificationResponse toMentorCertificationResponse(MentorCertification cert) {
        if (cert == null) return null;
        return MentorCertificationResponse.builder()
                .id(cert.getId())
                .name(cert.getName())
                .issuingOrganization(cert.getIssuingOrganization())
                .credentialId(cert.getCredentialId())
                .issuedDate(cert.getIssuedDate())
                .expiryDate(cert.getExpiryDate())
                .credentialUrl(cert.getCredentialUrl())
                .build();
    }

    public MentorProfileDetailResponse toMentorProfileDetailResponse(
            Mentor mentor,
            int currentInternCount,
            List<MentorSkill> skills,
            List<MentoringDomain> domains,
            List<MentorExperience> experiences,
            List<MentorCertification> certs) {

        if (mentor == null) return null;

        int maxInterns = mentor.getCapacity() != null ? mentor.getCapacity() : 5;
        int availableCapacity = Math.max(0, maxInterns - currentInternCount);

        List<MentorSkillResponse> skillResponses = skills != null ?
                skills.stream().map(this::toMentorSkillResponse).collect(Collectors.toList()) : Collections.emptyList();

        List<MentoringDomainResponse> domainResponses = domains != null ?
                domains.stream().map(this::toDomainResponse).collect(Collectors.toList()) : Collections.emptyList();

        List<MentorExperienceResponse> expResponses = experiences != null ?
                experiences.stream().map(this::toMentorExperienceResponse).collect(Collectors.toList()) : Collections.emptyList();

        List<MentorCertificationResponse> certResponses = certs != null ?
                certs.stream().map(this::toMentorCertificationResponse).collect(Collectors.toList()) : Collections.emptyList();

        return MentorProfileDetailResponse.builder()
                .id(mentor.getId())
                .userId(mentor.getUser() != null ? mentor.getUser().getId() : null)
                .employeeCode(mentor.getEmployeeCode())
                .fullName(mentor.getFullName())
                .email(mentor.getUser() != null ? mentor.getUser().getEmail() : null)
                .phone(mentor.getPhone() != null ? mentor.getPhone() : (mentor.getUser() != null ? mentor.getUser().getPhone() : null))
                .avatarUrl(mentor.getAvatarUrl())
                .departmentId(mentor.getDepartment() != null ? mentor.getDepartment().getId() : null)
                .departmentName(mentor.getDepartment() != null ? mentor.getDepartment().getName() : null)
                .jobTitle(mentor.getPosition() != null ? mentor.getPosition() : mentor.getTitle())
                .position(mentor.getPosition())
                .title(mentor.getTitle())
                .specialization(mentor.getSpecialization())
                .bio(mentor.getBio())
                .yearsOfExperience(mentor.getYearsOfExperience())
                .mentoringExperienceYears(mentor.getMentoringExperienceYears())
                .maxInterns(maxInterns)
                .currentInternCount(currentInternCount)
                .availableCapacity(availableCapacity)
                .status(mentor.getStatus())
                .profileStatus(mentor.getProfileStatus())
                .skills(skillResponses)
                .domains(domainResponses)
                .experiences(expResponses)
                .certifications(certResponses)
                .createdAt(mentor.getCreatedAt())
                .updatedAt(mentor.getUpdatedAt())
                .build();
    }
}
