package com.holaho.intern.shared.dto.hrm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmUserDto {
    private String employeeCode;
    private String fullName;
    private String email;
    private String department;
    private String position;
    private String status; // ACTIVE, TERMINATED
}

