package com.example.backend.dto.response;

import java.time.LocalDate;

public record InternScheduleResponse(
        Long internId,
        String internName,
        LocalDate internStartDate,
        LocalDate internEndDate,

        Long programId,
        String programName,
        LocalDate programStartDate,
        LocalDate programEndDate,

        Long groupId,
        String groupName
        // mentorName thêm sau vì ProgramGroup đang join Mentor entity
) {}
