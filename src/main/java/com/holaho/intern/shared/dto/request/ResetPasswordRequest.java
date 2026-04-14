package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Email khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Email(message = "Email khÃƒÂ´ng hÃ¡Â»Â£p lÃ¡Â»â€¡")
    private String email;

    @NotBlank(message = "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u mÃ¡Â»â€ºi khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Size(min = 6, message = "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u mÃ¡Â»â€ºi phÃ¡ÂºÂ£i cÃƒÂ³ ÃƒÂ­t nhÃ¡ÂºÂ¥t 6 kÃƒÂ½ tÃ¡Â»Â±")
    private String newPassword;

    // TODO: Add token field for email verification
    // private String resetToken;
}

