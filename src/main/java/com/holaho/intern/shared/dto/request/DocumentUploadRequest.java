package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentUploadRequest {
    @NotBlank(message = "LoÃ¡ÂºÂ¡i tÃƒÂ i liÃ¡Â»â€¡u khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private String type; // CV, APPLICATION_LETTER, TRANSCRIPT, etc.

    private Long internId; // For HR uploading on behalf of intern
}

