package com.holaho.intern.shared.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

    @Bean
    public MeterBinder customMetricsBinder() {
        return (MeterRegistry registry) -> {
            Counter.builder("audit.fallback.count")
                    .description("Count of emergency audit fallback logs written due to DB unavailability")
                    .register(registry);

            Counter.builder("idempotency.cache.hits")
                    .description("Count of duplicate write requests intercepted by X-Idempotency-Key filter")
                    .register(registry);

            Counter.builder("db.query.budget.exceeded")
                    .description("Count of requests exceeding maximum SQL query budget per endpoint")
                    .register(registry);
        };
    }
}
