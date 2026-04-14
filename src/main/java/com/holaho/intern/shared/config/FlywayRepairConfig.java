package com.holaho.intern.shared.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Use Flyway's own DataSource to avoid circular dependencies with Spring-managed beans
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate = 
                new org.springframework.jdbc.core.JdbcTemplate(flyway.getConfiguration().getDataSource());

            // Aggressively drop the offending constraints via direct JDBC
            String[] constraints = {
                "ALTER TABLE intern_documents DROP CHECK ck_intern_documents_status",
                "ALTER TABLE intern_documents DROP CHECK ck_intern_documents_type",
                "ALTER TABLE intern_documents DROP CONSTRAINT ck_intern_documents_status",
                "ALTER TABLE intern_documents DROP CONSTRAINT ck_intern_documents_type"
            };
            
            for (String sql : constraints) {
                try {
                    jdbcTemplate.execute(sql);
                } catch (Exception e) {
                    // Ignore "Constraint does not exist" errors
                }
            }

            // Repair the schema history table (removes failed migration rows)
            flyway.repair();
            // Then execute the migrations
            flyway.migrate();
        };
    }
}

