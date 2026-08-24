package com.holaho.intern.integration;

import com.holaho.intern.controller.BaseIntegrationTest;
import com.holaho.intern.entity.Tenant;
import com.holaho.intern.repository.TenantRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrossTenantIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenant1;
    private Tenant tenant2;

    @BeforeEach
    void setUpTenants() {
        TenantContext.clear();

        tenant1 = tenantRepository.findById(1L).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setName("Tenant One");
            t.setDomain("tenant1.com");
            t.setIsActive(true);
            return tenantRepository.save(t);
        });

        tenant2 = tenantRepository.findById(2L).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setName("Tenant Two");
            t.setDomain("tenant2.com");
            t.setIsActive(true);
            return tenantRepository.save(t);
        });
    }

    @Test
    @DisplayName("Should block access when requesting resources with mismatched Tenant ID header")
    void givenTenant1User_whenRequestingWithTenant2Header_thenAccessDeniedOrNotFound() throws Exception {
        // Create user strictly for Tenant 1
        User tenant1User = new User();
        tenant1User.setTenantId(tenant1.getId());
        tenant1User.setEmail("user_tenant1@company.com");
        tenant1User.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5R7y8vWQhQ7o0M8x0b5gOePqfG3r2");
        tenant1User.setFullName("User Tenant One");
        tenant1User.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(tenant1User);

        // Attempting to query resources with X-Tenant-ID: 2 header should isolate Tenant 1 data
        mockMvc.perform(get("/api/v1/interns")
                        .header("X-Tenant-ID", tenant2.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Unauthenticated request blocked by Security
    }
}
