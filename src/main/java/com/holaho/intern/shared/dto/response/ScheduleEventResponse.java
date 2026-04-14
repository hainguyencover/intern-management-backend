package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEventResponse {
    private Long id;
    private String title;
    private String date; // YYYY-MM-DD
    private String type; // TASK, MEETING, DEADLINE
    private String status;
}

