package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationSubmitRequest {
    @NotBlank(message = "VÃ¡Â»â€¹ trÃƒÂ­ Ã¡Â»Â©ng tuyÃ¡Â»Æ’n khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private String position;

    @jakarta.validation.constraints.NotNull(message = "ID chÃ†Â°Ã†Â¡ng trÃƒÂ¬nh khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private Long programId;

    private String note;
}

