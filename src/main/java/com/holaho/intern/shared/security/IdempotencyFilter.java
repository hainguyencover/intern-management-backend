package com.holaho.intern.shared.security;

import com.holaho.intern.shared.config.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (!WRITE_METHODS.contains(method) || !StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long tenantId = TenantContext.getCurrentTenantId();
        String redisKey = String.format("idempotency:tenant:%s:key:%s",
                tenantId != null ? tenantId : "default", idempotencyKey);

        String cachedResponseBody = redisTemplate.opsForValue().get(redisKey);
        if (cachedResponseBody != null) {
            log.info("Intercepted duplicate write request with X-Idempotency-Key: {}", idempotencyKey);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("X-Cache-Lookup", "HIT (Idempotent)");
            response.getWriter().write(cachedResponseBody);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            byte[] responseArray = responseWrapper.getContentAsByteArray();
            if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300 && responseArray.length > 0) {
                String responseBody = new String(responseArray, responseWrapper.getCharacterEncoding());
                redisTemplate.opsForValue().set(redisKey, responseBody, Duration.ofHours(24));
                log.debug("Cached idempotent response for key: {}", idempotencyKey);
            }
            responseWrapper.copyBodyToResponse();
        }
    }
}
