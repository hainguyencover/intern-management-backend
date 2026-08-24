package com.holaho.intern.mentor.dto;

import com.holaho.intern.mentor.enums.ProficiencyLevel;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorMatchFilterRequest {
    private String keyword;
    private Long departmentId;
    private String skill;
    private ProficiencyLevel minSkillLevel;
    private String domain;
    private Integer minExperience;
    private Boolean availableCapacityOnly;

    // Optional list of required skills for scoring
    private List<String> requiredSkills;
    private String targetDomain;
}
