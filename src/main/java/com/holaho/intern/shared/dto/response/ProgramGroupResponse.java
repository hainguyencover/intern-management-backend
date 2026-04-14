package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ProgramGroupResponse {
    private Long id;
    private Long programId;
    private String name;
    private GroupStatus status;
    private Long departmentId;
    private Long mentorId;
}

