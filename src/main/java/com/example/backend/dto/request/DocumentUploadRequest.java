package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentUploadRequest {
    @NotBlank(message = "Loại tài liệu không được để trống")
    private String type; // CV, APPLICATION_LETTER, TRANSCRIPT, etc.

    private Long internId; // For HR uploading on behalf of intern
}
