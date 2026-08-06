package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.LoginRequest;
import com.holaho.intern.shared.dto.request.RegisterRequest;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("integration@example.com");
        request.setPassword("password123");
        request.setFullName("Integration User");
        request.setStudentCode("ST_INT_01");
        request.setUniversity("Integration Uni");
        request.setMajor("Computer Science");

        mockMvc.perform(postWithTenant("/api/v1/auth/register", request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("integration@example.com"));
    }

    @Test
    void login_Success() throws Exception {
        // Prepare data
        User user = new User();
        user.setEmail("login@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName("Login User");
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("login@example.com")
                .password("password123")
                .build();

        mockMvc.perform(postWithTenant("/api/v1/auth/login", request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void login_Failure_BadCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("wrongpass")
                .build();

        mockMvc.perform(postWithTenant("/api/v1/auth/login", request))
                .andExpect(status().isNotFound()); // User not found returns 404 in current logic
    }
}
