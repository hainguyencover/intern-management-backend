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
public class ContractResponse {
    private Long id;
    private Long applicationId;
    private Long internId;
    private String internName;
    private String fileUrl;
    private String status;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
}
