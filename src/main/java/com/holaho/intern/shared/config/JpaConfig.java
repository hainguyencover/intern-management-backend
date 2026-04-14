package com.holaho.intern.shared.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * Register the {@link TenantInterceptor} as a Hibernate
     * {@link org.hibernate.resource.jdbc.spi.StatementInspector}
     * so that all SELECT queries are automatically filtered by tenant_id.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties ->
                hibernateProperties.put("hibernate.session_factory.statement_inspector",
                        new TenantInterceptor());
    }
}

