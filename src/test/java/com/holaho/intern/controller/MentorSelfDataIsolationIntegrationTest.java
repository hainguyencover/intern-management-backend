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
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class MentorSelfDataIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User mentorUserA;
    private User mentorUserB;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByCode("MENTOR").orElseGet(() -> {
            Role r = new Role();
            r.setCode("MENTOR");
            r.setName("Mentor Role");
            return roleRepository.save(r);
        });

        mentorUserA = new User();
        mentorUserA.setEmail("mentorA@company.com");
        mentorUserA.setFullName("Mentor User A");
        mentorUserA.setPasswordHash("hash123");
        mentorUserA.setRoles(Collections.singleton(role));
        mentorUserA = userRepository.save(mentorUserA);

        mentorUserB = new User();
        mentorUserB.setEmail("mentorB@company.com");
        mentorUserB.setFullName("Mentor User B");
        mentorUserB.setPasswordHash("hash123");
        mentorUserB.setRoles(Collections.singleton(role));
        mentorUserB = userRepository.save(mentorUserB);
    }

    private void authenticateAsMentor(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMyAssignedInterns_SelfDataIsolation() throws Exception {
        authenticateAsMentor(mentorUserA);

        mockMvc.perform(getWithTenant("/api/v1/interns/assigned-to-me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void mentorAccessAdminEndpoint_ReturnsForbidden() throws Exception {
        authenticateAsMentor(mentorUserA);

        mockMvc.perform(getWithTenant("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mentorAccessHrApplicationReview_ReturnsForbidden() throws Exception {
        authenticateAsMentor(mentorUserA);

        mockMvc.perform(postWithTenant("/api/v1/applications/1/review", Map.of("decision", "APPROVE", "comment", "Review by mentor")))
                .andExpect(status().isForbidden());
    }
}
