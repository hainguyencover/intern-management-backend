package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {
    private Long id;
    private Long applicationId;
    private Long internId;
    private String internName;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime hrConfirmedAt;
    private LocalDateTime internConfirmedAt;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private String revisionReason;
    private LocalDateTime revisionRequestedAt;
    private String documentHash;
    private String signedIp;
}
