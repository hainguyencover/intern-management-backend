package com.holaho.intern.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter();
    }

    @Test
    @DisplayName("Should bypass rate limiting for actuator endpoints")
    void givenActuatorRequest_whenDoFilter_thenBypass() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow requests under rate limit threshold")
    void givenNormalRequest_whenDoFilter_thenAllow() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/interns");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotEquals(429, response.getStatus());
    }

    @Test
    @DisplayName("Should reject requests with 429 when rate limit rate is exceeded")
    void givenExceededLimit_whenDoFilter_thenReturn429TooManyRequests() throws ServletException, IOException {
        String clientIp = "10.0.0.50";

        // Exceed limit of 100 requests per minute
        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/resource");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            rateLimitFilter.doFilter(req, resp, filterChain);
        }

        // 101st request should be blocked
        MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/api/v1/resource");
        blockedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResp = new MockHttpServletResponse();

        rateLimitFilter.doFilter(blockedReq, blockedResp, filterChain);

        assertEquals(429, blockedResp.getStatus());
        assertTrue(blockedResp.getContentAsString().contains("Quá nhiều yêu cầu"));
    }

    @Test
    @DisplayName("Should evict stale entries without throwing exception")
    void whenEvictStaleEntries_thenSuccess() {
        assertDoesNotThrow(() -> rateLimitFilter.evictStaleEntries());
    }
}
