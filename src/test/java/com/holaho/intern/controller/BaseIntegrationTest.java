package com.holaho.intern.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String TENANT_HEADER = "X-Tenant-ID";
    protected static final String DEFAULT_TENANT_ID = "1";

    /**
     * Helper to perform GET request with default tenant header pre-filled.
     */
    protected MockHttpServletRequestBuilder getWithTenant(String url) {
        return get(url).header(TENANT_HEADER, DEFAULT_TENANT_ID);
    }

    /**
     * Helper to perform POST request with default tenant header and json content.
     */
    protected MockHttpServletRequestBuilder postWithTenant(String url, Object body) throws Exception {
        return post(url)
                .header(TENANT_HEADER, DEFAULT_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Helper to perform PUT request with default tenant header and json content.
     */
    protected MockHttpServletRequestBuilder putWithTenant(String url, Object body) throws Exception {
        return put(url)
                .header(TENANT_HEADER, DEFAULT_TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    /**
     * Helper to perform DELETE request with default tenant header.
     */
    protected MockHttpServletRequestBuilder deleteWithTenant(String url) {
        return delete(url).header(TENANT_HEADER, DEFAULT_TENANT_ID);
    }
}
