package com.holaho.intern.mentor.entity;

import com.holaho.intern.mentor.enums.ProficiencyLevel;
import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "mentor_skills",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mentor_skill", columnNames = {"mentor_id", "skill_id"})
        }
)
public class MentorSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 30)
    private ProficiencyLevel proficiencyLevel;

    @Builder.Default
    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience = 0;
}
