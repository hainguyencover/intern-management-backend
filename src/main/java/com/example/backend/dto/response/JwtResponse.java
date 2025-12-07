package com.example.backend.dto.response;

import lombok.*;

import java.util.Set;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
}
