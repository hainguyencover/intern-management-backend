package com.holaho.intern.integration.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmEmployeeDto {
    private String externalId;
    private String fullName;
    private String email;
    private String phone;
    private String departmentCode;
    private String position;
    private LocalDate dateOfBirth;
    private String status;
}
