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
public class ProgramResponse {

    private Long id;
    private String code;
    private Long departmentId;
    private String departmentName;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProgramStatus status;
    private Integer maxInterns;
    private Long totalGroups;
    private Long totalInterns;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    // Constructor từ entity
    public ProgramResponse(com.holaho.intern.entity.Program program) {
        this.id = program.getId();
        this.code = program.getCode();
        this.departmentId = program.getDepartment() != null ? program.getDepartment().getId() : null;
        this.departmentName = program.getDepartment() != null ? program.getDepartment().getName() : null;
        this.name = program.getName();
        this.description = program.getDescription();
        this.startDate = program.getStartDate();
        this.endDate = program.getEndDate();
        this.status = program.getStatus();
        this.maxInterns = program.getMaxInterns();
        this.createdAt = program.getCreatedAt();
        this.updatedAt = program.getUpdatedAt();
    }
}
