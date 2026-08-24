package com.holaho.intern.shared.security;

import com.holaho.intern.shared.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private IdempotencyFilter idempotencyFilter;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should pass-through GET requests without idempotency check")
    void givenGetRequest_whenDoFilter_thenPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/interns");
        MockHttpServletResponse response = new MockHttpServletResponse();

        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should pass-through POST requests without X-Idempotency-Key header")
    void givenPostRequestWithoutHeader_whenDoFilter_thenPassThrough() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/interns");
        MockHttpServletResponse response = new MockHttpServletResponse();

        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Should return cached response when duplicate X-Idempotency-Key is present")
    void givenDuplicateRequest_whenDoFilter_thenReturnCachedResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        request.addHeader("X-Idempotency-Key", "unique-key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String cachedBody = "{\"success\":true,\"data\":{\"id\":100}}";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:tenant:1:key:unique-key-123")).thenReturn(cachedBody);

        idempotencyFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("HIT (Idempotent)", response.getHeader("X-Cache-Lookup"));
        assertEquals(cachedBody, response.getContentAsString());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Should execute request and cache response when valid new X-Idempotency-Key is provided")
    void givenNewRequest_whenDoFilter_thenCacheResponse() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        request.addHeader("X-Idempotency-Key", "new-key-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:tenant:1:key:new-key-456")).thenReturn(null);

        doAnswer(invocation -> {
            MockHttpServletResponse respWrapper = invocation.getArgument(1);
            respWrapper.setStatus(201);
            respWrapper.getWriter().write("{\"success\":true,\"data\":{\"id\":200}}");
            return null;
        }).when(filterChain).doFilter(any(), any());

        idempotencyFilter.doFilter(request, response, filterChain);

        verify(valueOperations, times(1)).set(
                eq("idempotency:tenant:1:key:new-key-456"),
                eq("{\"success\":true,\"data\":{\"id\":200}}"),
                eq(Duration.ofHours(24))
        );
        assertEquals(201, response.getStatus());
    }
}
