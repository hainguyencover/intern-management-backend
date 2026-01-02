package com.example.backend.dto.response;

import com.example.backend.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgramGroupResponse {
    private Long id;
    private Long programId;
    private String name;
    private GroupStatus status;
    private Long departmentId;
    private Long mentorId;
}
