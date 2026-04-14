package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorResponse {
    private String qrCodeUri;
    private String secret;
}

