package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotBlank(message = "MÃƒÂ£ xÃƒÂ¡c thÃ¡Â»Â±c khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private String code;
}

