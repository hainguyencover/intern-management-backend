package com.holaho.intern.mentor.dto;

import com.holaho.intern.mentor.enums.ProficiencyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMentorSkillRequest {

    @NotNull(message = "Trình độ không được để trống")
    private ProficiencyLevel proficiencyLevel;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
    @Max(value = 60, message = "Số năm kinh nghiệm không vượt quá 60 năm")
    private Integer yearsOfExperience;
}
