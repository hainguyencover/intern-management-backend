package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentVerifyRequest {
    @NotBlank(message = "VÃ¡Â»â€¹ trÃƒÂ­ Ã¡Â»Â©ng tuyÃ¡Â»Æ’n khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private String position;

    @Size(max = 1000)
    private String reviewNote;
}

