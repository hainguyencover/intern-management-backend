package com.holaho.intern.shared.config;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Component("tenantAwareKeyGenerator")
public class TenantAwareKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        Long tenantId = TenantContext.getCurrentTenantId();
        String tenantPrefix = (tenantId != null) ? "tenant:" + tenantId : "tenant:default";

        String paramKey = StringUtils.arrayToCommaDelimitedString(params);
        if (paramKey.isEmpty()) {
            paramKey = "all";
        }

        return tenantPrefix + ":" + method.getName() + ":" + paramKey;
    }
}
