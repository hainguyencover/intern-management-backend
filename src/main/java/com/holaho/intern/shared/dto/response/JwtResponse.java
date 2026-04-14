package com.holaho.intern.shared.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {
    private String token;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
    private Long id;
    private String email;
    private String fullName;
    private List<String> roles;

    public JwtResponse(String token, String refreshToken, Long id, String email, String fullName, List<String> roles) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
    }
}

