package com.holaho.intern.controller;

import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AiSearchIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User adminUser;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByCode("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setCode("ADMIN");
            r.setName("Admin Role");
            return roleRepository.save(r);
        });

        adminUser = new User();
        adminUser.setEmail("aisearch-admin@example.com");
        adminUser.setFullName("AI Search Admin");
        adminUser.setPasswordHash("hash");
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRoles(Collections.singleton(adminRole));
        adminUser = userRepository.save(adminUser);
    }

    @Test
    void searchInterns_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/search/interns")
                        .param("query", "test")
                        .principal(() -> adminUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void searchTasks_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/search/tasks")
                        .param("query", "test")
                        .principal(() -> adminUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void aiChat_Success() throws Exception {
        String chatRequestJson = """
                {
                    "message": "Hello AI Assistant"
                }
                """;

        mockMvc.perform(postWithTenant("/api/v1/ai/chat", chatRequestJson)
                        .principal(() -> adminUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void aiAnalyzeSentiment_Success() throws Exception {
        mockMvc.perform(postWithTenant("/api/v1/ai/analyze-sentiment", "")
                        .param("text", "I love this internship program!")
                        .principal(() -> adminUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
    }
}
