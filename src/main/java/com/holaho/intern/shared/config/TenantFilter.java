package com.holaho.intern.shared.config;

import com.holaho.intern.entity.Tenant;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP filter that extracts the tenant identifier from the request.
 * <p>
 * Resolution order:
 * <ol>
 *   <li>{@code X-Tenant-ID} header (for API clients)</li>
 *   <li>Falls back to default tenant (ID = 1) when header is absent</li>
 * </ol>
 * The resolved tenant ID is stored in {@link TenantContext} and cleared
 * after the request completes.
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Long DEFAULT_TENANT_ID = 1L;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        try {
            Long tenantId = resolveTenantId(request);
            TenantContext.setCurrentTenantId(tenantId);
            log.debug("Tenant context set to: {}", tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Long resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return Long.parseLong(header.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Tenant-ID header value: '{}', falling back to default", header);
            }
        }
        return DEFAULT_TENANT_ID;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip tenant resolution for public/static paths
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }
}

