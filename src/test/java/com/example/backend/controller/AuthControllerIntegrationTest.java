package com.example.backend.controller;

import com.example.backend.dto.request.LoginRequest;
import com.example.backend.dto.request.RegisterRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

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

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
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

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
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

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // Spring Security usually returns 403 on bad credentials by default
                                                    // if not handled
    }
}
