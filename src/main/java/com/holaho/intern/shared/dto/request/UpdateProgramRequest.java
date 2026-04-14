package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.ProgramStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProgramRequest {

    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private ProgramStatus status;
}

