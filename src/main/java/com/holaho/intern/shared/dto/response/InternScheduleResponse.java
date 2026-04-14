package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.ProgramGroup;
import com.holaho.intern.mentor.entity.Mentor;


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
        // mentorName thÃƒÂªm sau vÃƒÂ¬ ProgramGroup Ã„â€˜ang join Mentor entity
) {}

