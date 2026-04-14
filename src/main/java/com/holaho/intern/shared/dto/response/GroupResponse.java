package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.ProgramGroup;


import com.holaho.intern.shared.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private Long id;
    private Long programId;
    private String programName;
    private String name;
    private Long departmentId;
    private String departmentName;
    private Long mentorId;
    private String mentorName;
    private GroupStatus status;
    private Long totalMembers;
    private java.time.LocalDateTime createdAt;

    private java.time.LocalTime workStartTime;
    private java.time.LocalTime workEndTime;
    private String workDays;

    public GroupResponse(com.holaho.intern.entity.ProgramGroup group) {
        this.id = group.getId();
        this.programId = group.getProgram() != null ? group.getProgram().getId() : null;
        this.programName = group.getProgram() != null ? group.getProgram().getName() : null;
        this.name = group.getName();
        this.departmentId = group.getDepartmentId();
        this.mentorId = group.getMentorId();
        this.status = group.getStatus();
        this.createdAt = group.getCreatedAt();
        this.workStartTime = group.getWorkStartTime();
        this.workEndTime = group.getWorkEndTime();
        this.workDays = group.getWorkDays();
    }
}

