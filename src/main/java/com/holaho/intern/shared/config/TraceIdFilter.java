package com.holaho.intern.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(0) // Run before TenantFilter and other filters
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Check if trace ID was sent by upstream/gateway, otherwise generate new one
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        String spanId = UUID.randomUUID().toString().substring(0, 8);

        // Set in MDC for logging format patterns
        MDC.put(MDC_KEY, traceId);
        MDC.put("spanId", spanId);
        
        // Set in request attribute for GlobalExceptionHandler
        request.setAttribute(MDC_KEY, traceId);

        // Set in response headers for client tracking
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader("X-Span-ID", spanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            MDC.remove("spanId");
        }
    }
}
