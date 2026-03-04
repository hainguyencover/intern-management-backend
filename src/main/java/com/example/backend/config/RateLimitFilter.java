package com.example.backend.config;

import com.example.backend.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public RateLimitFilter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100)
                .timeoutDuration(Duration.ZERO)
                .build();
        this.rateLimiterRegistry = RateLimiterRegistry.of(config);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr();
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> rateLimiterRegistry.rateLimiter(k));

        if (limiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> apiResponse = ApiResponse.error(429, "Too many requests. Resilience4j limit exceeded.");
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            log.warn("Rate limit exceeded (Resilience4j) for IP: {}", key);
        }
    }
}
