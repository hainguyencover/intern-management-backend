package com.holaho.intern.controller;

import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.security.JwtTokenProvider;
import com.holaho.intern.shared.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class RbacIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String hrToken;
    private String internToken;

    @BeforeEach
    void setUp() {
        Role adminRole = getOrCreateRole("ADMIN");
        Role hrRole = getOrCreateRole("HR");
        Role internRole = getOrCreateRole("INTERN");

        User adminUser = createUser("admin_rbac@example.com", "Admin RBAC", adminRole);
        User hrUser = createUser("hr_rbac@example.com", "HR RBAC", hrRole);
        User internUser = createUser("intern_rbac@example.com", "Intern RBAC", internRole);

        adminToken = jwtTokenProvider.generateToken(new CustomUserDetails(adminUser));
        hrToken = jwtTokenProvider.generateToken(new CustomUserDetails(hrUser));
        internToken = jwtTokenProvider.generateToken(new CustomUserDetails(internUser));
    }

    private Role getOrCreateRole(String code) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(code);
                    role.setName(code + " Role");
                    return roleRepository.save(role);
                });
    }

    private User createUser(String email, String fullName, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFullName(fullName);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Collections.singleton(role));
        return userRepository.save(user);
    }

    @Test
    void adminAccess_AdminUsers_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void hrAccess_AdminUsers_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/admin/users")
                .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void internAccess_AdminUsers_Forbidden() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/admin/users")
                .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAccess_AuditLogs_Success() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void hrAccess_AuditLogs_Forbidden() throws Exception {
        mockMvc.perform(getWithTenant("/api/v1/admin/audit-logs")
                .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isForbidden()); // HR cannot access audit logs
    }
}
