package com.holaho.intern.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.holaho.intern.entity.Tenant;
import com.holaho.intern.repository.TenantRepository;
import com.holaho.intern.shared.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(TENANT_HEADER);

        // 1. Missing Header
        if (header == null || header.isBlank()) {
            log.warn("Missing X-Tenant-ID header for request: {}", request.getRequestURI());
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, "TENANT_MISSING",
                    "Yêu cầu cần có Header X-Tenant-ID để định danh doanh nghiệp", request.getRequestURI());
            return;
        }

        // 2. Parse ID
        Long tenantId;
        try {
            tenantId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid X-Tenant-ID format: '{}' for request: {}", header, request.getRequestURI());
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, "INVALID_TENANT_FORMAT",
                    "Header X-Tenant-ID không đúng định dạng số", request.getRequestURI());
            return;
        }

        // 3. Database Check
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            log.warn("Tenant ID {} not found for request: {}", tenantId, request.getRequestURI());
            writeErrorResponse(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND",
                    "Doanh nghiệp không tồn tại trên hệ thống", request.getRequestURI());
            return;
        }

        Tenant tenant = tenantOpt.get();

        // 4. Status Check
        if (Boolean.FALSE.equals(tenant.getIsActive())) {
            log.warn("Tenant ID {} is disabled. Request blocked for: {}", tenantId, request.getRequestURI());
            writeErrorResponse(response, HttpStatus.FORBIDDEN, "TENANT_DISABLED",
                    "Tài khoản doanh nghiệp hiện đang bị tạm khóa", request.getRequestURI());
            return;
        }

        // 5. Set Context
        try {
            TenantContext.setCurrentTenantId(tenantId);
            log.debug("Tenant context set to: {}", tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> apiResponse = ApiResponse.error(code, message, null, path);
        
        // Ensure ObjectMapper handles Java 8 date/time types
        ObjectMapper localMapper = objectMapper.copy();
        localMapper.registerModule(new JavaTimeModule());
        
        response.getWriter().write(localMapper.writeValueAsString(apiResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/api/v1/internal/diag");
    }
}
