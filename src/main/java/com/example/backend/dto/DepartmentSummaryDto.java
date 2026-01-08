package com.example.backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class DepartmentSummaryDto {
    private Long id;
    private String code;
    private String name;
}
