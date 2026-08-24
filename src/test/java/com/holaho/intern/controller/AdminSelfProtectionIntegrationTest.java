package com.holaho.intern.controller;

import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AdminSelfProtectionIntegrationTest extends BaseIntegrationTest {

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
        adminUser.setEmail("admin-test@company.com");
        adminUser.setFullName("System Administrator");
        adminUser.setPasswordHash("superSecretHash123");
        adminUser.setRoles(Collections.singleton(adminRole));
        adminUser = userRepository.save(adminUser);
    }

    private void authenticateAsAdmin() {
        CustomUserDetails details = new CustomUserDetails(adminUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void adminAccessUserManagement_Success() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(getWithTenant("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminGetUsers_DoesNotExposeSecrets() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(getWithTenant("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("superSecretHash123"))))
                .andExpect(content().string(not(containsString("refreshToken"))))
                .andExpect(content().string(not(containsString("jwtSecret"))));
    }

    @Test
    void nonAdminAccessAuditLogs_ReturnsForbidden() throws Exception {
        User internUser = new User();
        internUser.setId(999L);
        internUser.setEmail("intern@student.com");
        internUser.setPasswordHash("hash");
        internUser.setStatus(com.holaho.intern.shared.enums.UserStatus.ACTIVE);
        Role internRole = new Role();
        internRole.setCode("INTERN");
        internUser.setRoles(Collections.singleton(internRole));

        CustomUserDetails internDetails = new CustomUserDetails(internUser);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(internDetails, null, internDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(getWithTenant("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
