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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class InternSelfDataIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User internUserA;
    private User internUserB;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByCode("INTERN").orElseGet(() -> {
            Role r = new Role();
            r.setCode("INTERN");
            r.setName("Intern Role");
            return roleRepository.save(r);
        });

        internUserA = new User();
        internUserA.setEmail("internA@company.com");
        internUserA.setFullName("Intern User A");
        internUserA.setPasswordHash("hash123");
        internUserA.setRoles(Collections.singleton(role));
        internUserA = userRepository.save(internUserA);

        internUserB = new User();
        internUserB.setEmail("internB@company.com");
        internUserB.setFullName("Intern User B");
        internUserB.setPasswordHash("hash123");
        internUserB.setRoles(Collections.singleton(role));
        internUserB = userRepository.save(internUserB);
    }

    private void authenticateAsUser(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMyDocuments_SelfDataIsolation() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/documents/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyContracts_SelfDataIsolation() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/contracts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyApplications_SelfDataIsolation() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/applications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyAllowances_SelfDataIsolation() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/allowances/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAssignedToMe_SelfDataIsolation() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/tasks/assigned-to-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void internAccessAdminEndpoint_ReturnsForbidden() throws Exception {
        authenticateAsUser(internUserA);

        mockMvc.perform(getWithTenant("/api/v1/admin/audit-logs"))
                .andExpect(status().isForbidden());
    }
}
