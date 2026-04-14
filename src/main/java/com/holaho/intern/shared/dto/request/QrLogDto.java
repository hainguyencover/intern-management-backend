package com.holaho.intern.shared.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrLogDto {
    private String employeeCode; // studentCode or email
    private LocalDateTime timestamp;
    private String deviceId;
}

