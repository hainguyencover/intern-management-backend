package com.example.backend.dto.response;

import com.example.backend.enums.GroupStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupResponse {
    private Long id;
    private Long programId;
    private Long departmentId;
    private Long mentorId;
    private String mentorName;

    private String name;
    private GroupStatus status;

    private Long memberCount;
}
