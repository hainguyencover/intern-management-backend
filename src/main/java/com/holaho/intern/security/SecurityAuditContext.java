package com.holaho.intern.security;

import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

public final class SecurityAuditContext {

    private SecurityAuditContext() {
        // utility class
    }

    public static Long getActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }

    public static String getActorUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof User user) {
                return user.getUsername();
            }
            return auth.getName();
        }
        return "SYSTEM";
    }

    public static String getActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getEmail();
        }
        return null;
    }

    public static String getActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            return auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .collect(Collectors.joining(","));
        }
        return "ANONYMOUS";
    }

    public static Long getTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    public static String getRequestId() {
        String reqId = MDC.get(RequestIdFilter.MDC_REQUEST_ID_KEY);
        if (reqId != null) {
            return reqId;
        }
        HttpServletRequest req = getHttpServletRequest();
        if (req != null) {
            return req.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        }
        return null;
    }

    public static String getIpAddress() {
        HttpServletRequest req = getHttpServletRequest();
        if (req == null) return "INTERNAL";
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public static String getUserAgent() {
        HttpServletRequest req = getHttpServletRequest();
        if (req == null) return "SYSTEM";
        String ua = req.getHeader("User-Agent");
        return ua != null ? ua : "UNKNOWN";
    }

    private static HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
