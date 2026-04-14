package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.GroupStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramGroupCreateRequest {

    @NotNull
    private Long programId;

    @NotBlank
    private String name;

    // optional
    private Long departmentId; // nullable
    private Long mentorId;     // nullable

    // optional, default ACTIVE
    private GroupStatus status = GroupStatus.ACTIVE;
}

