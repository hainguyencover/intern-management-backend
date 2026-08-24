package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.MentorMatchFilterRequest;
import com.holaho.intern.mentor.dto.MentorMatchResultResponse;
import com.holaho.intern.mentor.dto.MentorProfileDetailResponse;
import com.holaho.intern.mentor.dto.MentorSkillResponse;

import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorMatchingServiceImpl implements MentorMatchingService {

    private final MentorRepository mentorRepository;
    private final MentorProfileService mentorProfileService;

    @Override
    @Transactional(readOnly = true)
    public List<MentorMatchResultResponse> matchMentorsForHr(Long tenantId, MentorMatchFilterRequest filter) {
        List<Mentor> mentors = mentorRepository.findAllByTenantIdWithDetails(tenantId);

        List<MentorMatchResultResponse> results = new ArrayList<>();

        for (Mentor mentor : mentors) {
            MentorProfileDetailResponse profile = mentorProfileService.getMentorProfileForHr(tenantId, mentor.getId());

            // Apply filter checks
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String kw = filter.getKeyword().toLowerCase();
                boolean nameMatch = profile.getFullName() != null && profile.getFullName().toLowerCase().contains(kw);
                boolean titleMatch = profile.getJobTitle() != null && profile.getJobTitle().toLowerCase().contains(kw);
                boolean specMatch = profile.getSpecialization() != null && profile.getSpecialization().toLowerCase().contains(kw);
                if (!nameMatch && !titleMatch && !specMatch) {
                    continue;
                }
            }

            if (filter.getDepartmentId() != null && !filter.getDepartmentId().equals(profile.getDepartmentId())) {
                continue;
            }

            if (Boolean.TRUE.equals(filter.getAvailableCapacityOnly()) && profile.getAvailableCapacity() <= 0) {
                continue;
            }

            if (filter.getMinExperience() != null) {
                int mentorExp = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().intValue() : 0;
                if (mentorExp < filter.getMinExperience()) {
                    continue;
                }
            }

            // Calculate matching scores
            double skillScore = calculateSkillScore(profile, filter);
            double expScore = calculateExperienceScore(profile);
            double domainScore = calculateDomainScore(profile, filter);
            double capacityScore = calculateCapacityScore(profile);

            double totalPercentage = (skillScore * 0.60) + (expScore * 0.20) + (domainScore * 0.10) + (capacityScore * 0.10);

            List<String> matchedSkills = getMatchedSkillNames(profile, filter);
            List<String> matchedDomains = profile.getDomains() != null ?
                    profile.getDomains().stream().map(d -> d.getName()).collect(Collectors.toList()) : Collections.emptyList();

            String reason = String.format("Skill: %.0f%% | Exp: %.0f%% | Capacity: %d slot(s)",
                    skillScore, expScore, profile.getAvailableCapacity());

            results.add(MentorMatchResultResponse.builder()
                    .mentor(profile)
                    .matchPercentage((int) Math.round(totalPercentage))
                    .skillMatchScore(skillScore)
                    .experienceMatchScore(expScore)
                    .domainMatchScore(domainScore)
                    .capacityMatchScore(capacityScore)
                    .matchedSkills(matchedSkills)
                    .matchedDomains(matchedDomains)
                    .matchReason(reason)
                    .build());
        }

        results.sort(Comparator.comparing(MentorMatchResultResponse::getMatchPercentage).reversed());
        return results;
    }

    private double calculateSkillScore(MentorProfileDetailResponse profile, MentorMatchFilterRequest filter) {
        if (filter.getRequiredSkills() != null && !filter.getRequiredSkills().isEmpty()) {
            double sumWeight = 0.0;
            Map<String, MentorSkillResponse> skillMap = new HashMap<>();
            if (profile.getSkills() != null) {
                for (MentorSkillResponse ms : profile.getSkills()) {
                    skillMap.put(ms.getName().toLowerCase(), ms);
                }
            }

            for (String reqSkill : filter.getRequiredSkills()) {
                MentorSkillResponse ms = skillMap.get(reqSkill.toLowerCase());
                if (ms != null && ms.getProficiencyLevel() != null) {
                    switch (ms.getProficiencyLevel()) {
                        case EXPERT -> sumWeight += 1.0;
                        case ADVANCED -> sumWeight += 0.8;
                        case INTERMEDIATE -> sumWeight += 0.6;
                        case BEGINNER -> sumWeight += 0.4;
                    }
                }
            }
            return (sumWeight / filter.getRequiredSkills().size()) * 100.0;
        }

        if (filter.getSkill() != null && !filter.getSkill().isBlank()) {
            if (profile.getSkills() != null) {
                for (MentorSkillResponse ms : profile.getSkills()) {
                    if (ms.getName().equalsIgnoreCase(filter.getSkill().trim())) {
                        if (ms.getProficiencyLevel() != null) {
                            return switch (ms.getProficiencyLevel()) {
                                case EXPERT -> 100.0;
                                case ADVANCED -> 85.0;
                                case INTERMEDIATE -> 70.0;
                                case BEGINNER -> 50.0;
                            };
                        }
                        return 80.0;
                    }
                }
            }
            return 0.0;
        }

        return (profile.getSkills() != null && !profile.getSkills().isEmpty()) ? 100.0 : 50.0;
    }

    private double calculateExperienceScore(MentorProfileDetailResponse profile) {
        int years = profile.getYearsOfExperience() != null ? profile.getYearsOfExperience().intValue() : 0;
        if (years >= 5) return 100.0;
        if (years >= 3) return 80.0;
        if (years >= 1) return 60.0;
        return 40.0;
    }

    private double calculateDomainScore(MentorProfileDetailResponse profile, MentorMatchFilterRequest filter) {
        if (filter.getTargetDomain() != null && !filter.getTargetDomain().isBlank()) {
            if (profile.getDomains() != null) {
                boolean hasDomain = profile.getDomains().stream()
                        .anyMatch(d -> d.getName().equalsIgnoreCase(filter.getTargetDomain().trim()));
                if (hasDomain) return 100.0;
            }
            return 50.0;
        }
        return 100.0;
    }

    private double calculateCapacityScore(MentorProfileDetailResponse profile) {
        int capacity = profile.getAvailableCapacity() != null ? profile.getAvailableCapacity() : 0;
        if (capacity >= 3) return 100.0;
        if (capacity == 2) return 80.0;
        if (capacity == 1) return 50.0;
        return 0.0;
    }

    private List<String> getMatchedSkillNames(MentorProfileDetailResponse profile, MentorMatchFilterRequest filter) {
        if (profile.getSkills() == null) return Collections.emptyList();
        if (filter.getRequiredSkills() != null && !filter.getRequiredSkills().isEmpty()) {
            Set<String> reqSet = filter.getRequiredSkills().stream().map(String::toLowerCase).collect(Collectors.toSet());
            return profile.getSkills().stream()
                    .filter(s -> reqSet.contains(s.getName().toLowerCase()))
                    .map(MentorSkillResponse::getName)
                    .collect(Collectors.toList());
        }
        return profile.getSkills().stream().map(MentorSkillResponse::getName).collect(Collectors.toList());
    }
}
