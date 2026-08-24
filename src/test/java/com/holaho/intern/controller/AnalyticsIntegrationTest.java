package com.holaho.intern.controller;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

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
        Role internRole = roleRepository.findByCode("INTERN").orElseGet(() -> {
            Role r = new Role();
            r.setCode("INTERN");
            r.setName("Intern");
            return roleRepository.save(r);
        });
        Role hrRole = roleRepository.findByCode("HR").orElseGet(() -> {
            Role r = new Role();
            r.setCode("HR");
            r.setName("HR");
            return roleRepository.save(r);
        });

        // HR
        hrUser = new User();
        hrUser.setEmail("analytics-hr@example.com");
        hrUser.setFullName("HR Analytics");
        hrUser.setPasswordHash("hash");
        hrUser.setStatus(UserStatus.ACTIVE);
        hrUser.setRoles(Collections.singleton(hrRole));
        hrUser = userRepository.save(hrUser);

        // Intern
        internUser = new User();
        internUser.setEmail("analytics-intern@example.com");
        internUser.setFullName("Intern Analytics");
        internUser.setPasswordHash("hash");
        internUser.setStatus(UserStatus.ACTIVE);
        internUser.setRoles(Collections.singleton(internRole));
        internUser = userRepository.save(internUser);

        internProfile = new InternProfile();
        internProfile.setUser(internUser);
        internProfile.setUniversity("Đại học Bách Khoa");
        internProfile.setMajor("Computer Science");
        internProfile.setStatus("COMPLETED");
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

    @Test
    void getAnalyticsOverview_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/overview")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalInterns").value(1))
                .andExpect(jsonPath("$.data.completedInterns").value(1))
                .andExpect(jsonPath("$.data.completionRate").value(100.0));
    }

    @Test
    void getAnalyticsBySchool_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/interns/by-school")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].schoolName").value("Đại học Bách Khoa"))
                .andExpect(jsonPath("$.data[0].count").value(1));
    }

    @Test
    void getAnalyticsByMajor_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/interns/by-major")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].majorName").value("Computer Science"))
                .andExpect(jsonPath("$.data[0].count").value(1));
    }

    @Test
    void getAnalyticsCompletion_AsHr_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/completion")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(1))
                .andExpect(jsonPath("$.data.completionRate").value(100.0));
    }

    @Test
    void getAnalyticsOverview_AsIntern_Forbidden() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/overview")
                        .principal(() -> internUser.getEmail()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAnalyticsOverview_InvalidDateFilter_BadRequest() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/hr/analytics/overview?fromDate=2026-08-30&toDate=2026-08-01")
                        .principal(() -> hrUser.getEmail()))
                .andExpect(status().isBadRequest());
    }
}
