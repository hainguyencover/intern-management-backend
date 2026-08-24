package com.holaho.intern.mentor.dto;

import com.holaho.intern.mentor.enums.ProficiencyLevel;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorSkillResponse {
    private Long id;
    private Long skillId;
    private String name;
    private String category;
    private ProficiencyLevel proficiencyLevel;
    private Integer yearsOfExperience;
}
