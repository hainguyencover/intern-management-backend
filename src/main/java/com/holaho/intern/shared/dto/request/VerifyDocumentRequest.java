package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyDocumentRequest {

    @NotBlank(message = "Decision is required")
    private String decision; // APPROVE or REJECT

    @Size(max = 1000)
    private String note;
}

