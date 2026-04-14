package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    private String name;
    private Long mentorId;
    private GroupStatus status;

    private java.time.LocalTime workStartTime;
    private java.time.LocalTime workEndTime;
    private String workDays;
}

