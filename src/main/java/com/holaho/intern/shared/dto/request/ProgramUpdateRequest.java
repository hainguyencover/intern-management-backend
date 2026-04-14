package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProgramUpdateRequest {

    // optional: cho phÃƒÂ©p Ã„â€˜Ã¡Â»â€¢i department (nÃ¡ÂºÂ¿u bÃ¡ÂºÂ¡n muÃ¡Â»â€˜n khÃƒÂ³a thÃƒÂ¬ bÃ¡Â»Â field nÃƒÂ y)
    private Long departmentId;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
}

