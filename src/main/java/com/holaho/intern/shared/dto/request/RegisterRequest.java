package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Email(message = "Email khÃƒÂ´ng hÃ¡Â»Â£p lÃ¡Â»â€¡")
    private String email;

    @NotBlank(message = "HÃ¡Â»Â tÃƒÂªn khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    private String fullName;

    @NotBlank(message = "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u khÃƒÂ´ng Ã„â€˜Ã†Â°Ã¡Â»Â£c Ã„â€˜Ã¡Â»Æ’ trÃ¡Â»â€˜ng")
    @Size(min = 6, message = "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u phÃ¡ÂºÂ£i cÃƒÂ³ ÃƒÂ­t nhÃ¡ÂºÂ¥t 6 kÃƒÂ½ tÃ¡Â»Â±")
    private String password;

    private String phone;

    // Additional Intern Info
    private Integer dobYear;
    private String address;
    private String studentCode;
    private String university;
    private String major;
    private Integer startYear;
    private Integer endYear;
}

