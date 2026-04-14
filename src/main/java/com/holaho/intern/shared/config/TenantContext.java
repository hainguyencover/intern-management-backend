package com.holaho.intern.shared.config;

/**
 * ThreadLocal holder for the current tenant ID.
 * Set by {@link TenantFilter} on each request and cleared after the request completes.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    public static Long getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

