package com.holaho.intern.controller;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AnalyticsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private InternProfileRepository internProfileRepository;

    private User hrUser;
    private User internUser;
    private InternProfile internProfile;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByCode("INTERN").isEmpty()) {
            Role r = new Role();
            r.setCode("INTERN");
            r.setName("Intern");
            roleRepository.save(r);
        }
        if (roleRepository.findByCode("HR").isEmpty()) {
            Role r = new Role();
            r.setCode("HR");
            r.setName("HR");
            roleRepository.save(r);
        }

        // HR
        hrUser = new User();
        hrUser.setEmail("analytics-hr@example.com");
        hrUser.setFullName("HR Analytics");
        hrUser.setPasswordHash("hash");
        hrUser.setStatus(UserStatus.ACTIVE);
        hrUser = userRepository.save(hrUser);

        // Intern
        internUser = new User();
        internUser.setEmail("analytics-intern@example.com");
        internUser.setFullName("Intern Analytics");
        internUser.setPasswordHash("hash");
        internUser.setStatus(UserStatus.ACTIVE);
        internUser = userRepository.save(internUser);

        internProfile = new InternProfile();
        internProfile.setUser(internUser);
        internProfile.setUniversity("Hust");
        internProfile.setMajor("CS");
        internProfile = internProfileRepository.save(internProfile);
    }

    @Test
    void getDashboardOverview_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/dashboard/overview")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInterns").exists());
    }

    @Test
    void getUniversityStats_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/dashboard/university-stats")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getInternDashboard_AsIntern_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/dashboard/intern")
                        .principal(() -> internUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.internName").value("Intern Analytics"));
    }
}
