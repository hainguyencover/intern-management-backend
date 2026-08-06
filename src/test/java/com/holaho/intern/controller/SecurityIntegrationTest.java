package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.entity.RefreshToken;
import com.holaho.intern.repository.RefreshTokenRepository;
import com.holaho.intern.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByCode("INTERN").isEmpty()) {
            Role internRole = new Role();
            internRole.setCode("INTERN");
            internRole.setName("Intern Role");
            roleRepository.save(internRole);
        }
    }

    @Test
    void passwordPolicy_WeakPassword_BadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("weak@example.com");
        request.setFullName("Weak User");
        request.setPassword("123"); // Weak password (less than 8 chars, no upper, no special)
        request.setStudentCode("ST_WEAK");

        mockMvc.perform(postWithTenant("/api/v1/auth/register", request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void accountLock_After5FailedAttempts_LocksAccount() throws Exception {
        // Create user
        User user = new User();
        user.setEmail("lockout@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("Lockout User");
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest badRequest = LoginRequest.builder()
                .email("lockout@example.com")
                .password("wrongpassword")
                .build();

        // 5 bad login attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(postWithTenant("/api/v1/auth/login", badRequest))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should be locked (returns 400 Bad Request from lockout check)
        mockMvc.perform(postWithTenant("/api/v1/auth/login", badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Tài khoản đã bị khoá tạm thời")));
    }

    @Test
    void rtr_ReplayAttack_RevokesAllTokens() throws Exception {
        // Create user
        User user = new User();
        user.setEmail("rtr@example.com");
        user.setPasswordHash(passwordEncoder.encode("Password123!"));
        user.setFullName("RTR User");
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        // Generate initial refresh token
        RefreshToken rt = authService.createRefreshToken(user.getId());
        String tokenStr = rt.getToken();

        com.holaho.intern.shared.dto.request.TokenRefreshRequest refreshRequest =
                new com.holaho.intern.shared.dto.request.TokenRefreshRequest(tokenStr);

        // First refresh should succeed and rotate token
        mockMvc.perform(postWithTenant("/api/v1/auth/refresh", refreshRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // Second refresh with the SAME old token (Replay Attack) should return 401 Unauthorized
        mockMvc.perform(postWithTenant("/api/v1/auth/refresh", refreshRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Cảnh báo bảo mật")));
    }
}
