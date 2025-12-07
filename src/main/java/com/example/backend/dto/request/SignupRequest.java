package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class SignupRequest {
    @NotBlank @Size(min=3, max=100)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min=6)
    private String password;

    private String fullName;

    @NotNull
    private Set<String> roles; // expected: ADMIN, HR, MENTOR, INTERN
}
