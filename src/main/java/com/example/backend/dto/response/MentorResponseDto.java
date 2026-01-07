package com.example.backend.dto.response;

import com.example.backend.dto.DepartmentSummaryDto;
import com.example.backend.dto.UserSummaryDto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MentorResponseDto {
    private Long id;
    private String title;
    private UserSummaryDto user;
    private DepartmentSummaryDto department;
}
