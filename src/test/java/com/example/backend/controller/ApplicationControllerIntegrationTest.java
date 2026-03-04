package com.example.backend.controller;

import com.example.backend.dto.request.ApplicationSubmitRequest;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
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

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApplicationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private String internToken;
    private User internUser;

    @BeforeEach
    void setUp() {
        Role internRole = roleRepository.findByCode("INTERN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("INTERN");
                    role.setName("Intern Role");
                    return roleRepository.save(role);
                });

        internUser = new User();
        internUser.setEmail("intern_test@example.com");
        internUser.setPasswordHash(passwordEncoder.encode("password123"));
        internUser.setFullName("Intern Test");
        internUser.setStatus(UserStatus.ACTIVE);
        internUser.setRoles(Collections.singleton(internRole));
        internUser = userRepository.save(internUser);

        InternProfile profile = new InternProfile();
        profile.setUser(internUser);
        profile.setStudentCode("ST_TEST_01");
        internProfileRepository.save(profile);

        // Generate token for the intern user
        com.example.backend.security.CustomUserDetails userDetails = new com.example.backend.security.CustomUserDetails(
                internUser);
        internToken = jwtTokenProvider.generateToken(userDetails);
    }

    @Test
    void submitApplication_Success() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setPosition("Java Developer Intern");
        request.setNote("I love coding!");

        mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + internToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.position").value("Java Developer Intern"));
    }

    @Test
    void submitApplication_Unauthorized() throws Exception {
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.setPosition("Java Developer Intern");

        mockMvc.perform(post("/api/v1/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
