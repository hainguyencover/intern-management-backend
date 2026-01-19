package com.example.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long internId;
    private String internName;
    private String type;
    private String fileName;
    private String fileUrl;
    private String status;
    private LocalDateTime uploadedAt;
    private Long reviewedBy;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
}
