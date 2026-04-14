package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApplicationRequest {

    @Size(max = 255)
    private String position;

    @Size(max = 1000, message = "{application.note.size}")
    private String note;

    private Long programId; // Optional: nÃ¡ÂºÂ¿u apply vÃƒÂ o program cÃ¡Â»Â¥ thÃ¡Â»Æ’
}

