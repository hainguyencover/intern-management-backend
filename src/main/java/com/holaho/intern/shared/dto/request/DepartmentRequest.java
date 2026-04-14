package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {
    @NotBlank(message = "MÃƒÂ£ phÃƒÂ²ng ban khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Size(max = 50, message = "MÃƒÂ£ phÃƒÂ²ng ban khÃƒÂ´ng quÃƒÂ¡ 50 kÃƒÂ½ tÃ¡Â»Â±")
    private String code;

    @NotBlank(message = "TÃƒÂªn phÃƒÂ²ng ban khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Size(max = 255, message = "TÃƒÂªn phÃƒÂ²ng ban khÃƒÂ´ng quÃƒÂ¡ 255 kÃƒÂ½ tÃ¡Â»Â±")
    private String name;

    @Size(max = 1000, message = "MÃƒÂ´ tÃ¡ÂºÂ£ khÃƒÂ´ng quÃƒÂ¡ 1000 kÃƒÂ½ tÃ¡Â»Â±")
    private String description;
}

