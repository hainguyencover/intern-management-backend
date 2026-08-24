package com.holaho.intern.mentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorExperienceRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 150, message = "Tên công ty không vượt quá 150 ký tự")
    private String companyName;

    @NotBlank(message = "Vị trí công việc không được để trống")
    @Size(max = 150, message = "Vị trí không vượt quá 150 ký tự")
    private String position;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 2000, message = "Mô tả công việc không vượt quá 2000 ký tự")
    private String description;

    private Boolean isCurrent;
}
