package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.Program;


import com.holaho.intern.shared.enums.ProgramStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramListItemResponse {

    private Long id;
    private String name;
    private String departmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProgramStatus status;
    private Long totalGroups;

    public ProgramListItemResponse(com.holaho.intern.entity.Program program, Long totalGroups) {
        this.id = program.getId();
        this.name = program.getName();
        this.departmentName = program.getDepartment() != null ? program.getDepartment().getName() : null;
        this.startDate = program.getStartDate();
        this.endDate = program.getEndDate();
        this.status = program.getStatus();
        this.totalGroups = totalGroups;
    }
}

