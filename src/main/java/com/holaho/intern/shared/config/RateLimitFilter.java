package com.holaho.intern.shared.config;

import com.holaho.intern.shared.dto.response.ApiResponse;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiter using Resilience4j with automatic cleanup
 * to prevent unbounded memory growth from unique IP addresses.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterConfig rateLimiterConfig;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTimes = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /** Evict entries not accessed for 10 minutes */
    private static final long EVICTION_MILLIS = Duration.ofMinutes(10).toMillis();

    public RateLimitFilter() {
        this.rateLimiterConfig = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .limitForPeriod(100)
                .timeoutDuration(Duration.ZERO)
                .build();
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
        lastAccessTimes.put(key, System.currentTimeMillis());

        RateLimiter limiter = limiters.computeIfAbsent(key,
                k -> RateLimiter.of(k, rateLimiterConfig));

        if (limiter.acquirePermission()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> apiResponse = ApiResponse.error(429,
                    "Quá nhiều yêu cầu. Vui lòng thử lại sau.",
                    request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            log.warn("Rate limit exceeded for IP: {}", key);
        }
    }

    /**
     * Scheduled cleanup: evict stale entries every 5 minutes to prevent
     * unbounded ConcurrentHashMap growth from unique client IPs.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void evictStaleEntries() {
        long now = System.currentTimeMillis();
        int removed = 0;

        var iterator = lastAccessTimes.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue() > EVICTION_MILLIS) {
                iterator.remove();
                limiters.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Evicted {} stale rate-limiter entries. Active: {}", removed, limiters.size());
        }
    }
}
