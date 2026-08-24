package com.holaho.intern.mentor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMentorProfileRequest {

    @Size(max = 150, message = "Họ tên không vượt quá 150 ký tự")
    private String fullName;

    @Size(max = 100, message = "Chức danh không vượt quá 100 ký tự")
    private String jobTitle;

    @Size(max = 150, message = "Phòng ban không vượt quá 150 ký tự")
    private String department;

    @Size(max = 255, message = "Chuyên môn không vượt quá 255 ký tự")
    private String specialization;

    @Size(max = 2000, message = "Giới thiệu bản thân không vượt quá 2000 ký tự")
    private String bio;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
    @Max(value = 60, message = "Số năm kinh nghiệm không vượt quá 60 năm")
    private Integer yearsOfExperience;

    @Min(value = 0, message = "Kinh nghiệm hướng dẫn không được âm")
    @Max(value = 60, message = "Kinh nghiệm hướng dẫn không vượt quá 60 năm")
    private Integer mentoringExperienceYears;

    @Min(value = 1, message = "Số TTS tối đa ít nhất là 1")
    @Max(value = 100, message = "Số TTS tối đa không vượt quá 100")
    private Integer maxInterns;

    private List<Long> domainIds;
}
