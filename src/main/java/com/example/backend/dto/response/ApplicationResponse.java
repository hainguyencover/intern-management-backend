package com.example.backend.dto.response;

import com.example.backend.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationResponse {
    private Long id;
    private Long internId;
    private String position;
    private LocalDateTime appliedAt;
    private ApplicationStatus status;
    private String note;
}
