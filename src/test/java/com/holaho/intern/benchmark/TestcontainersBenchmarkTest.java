package com.holaho.intern.benchmark;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"benchmark", "benchmark-seed"})
@Testcontainers
public class TestcontainersBenchmarkTest {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.35")
            .withDatabaseName("management_intern")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Testcontainers MySQL 8 Pipeline & Deterministic Seed Counts")
    void testTestcontainersPipelineAndSeedIntegrity() {
        assertTrue(mysqlContainer.isRunning(), "MySQL 8 Testcontainer must be running");

        // 1. Verify Dataset Counts
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        Integer internCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM intern_profiles", Integer.class);
        Integer reportCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM weekly_reports", Integer.class);
        Integer taskCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasks", Integer.class);
        Integer evalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evaluations", Integer.class);

        assertNotNull(userCount);
        assertTrue(userCount >= 50, "Users seeded");
        assertTrue(internCount > 0, "Intern profiles seeded");
        assertTrue(reportCount > 0, "Weekly reports seeded");

        // 2. Verify Referential Integrity (Zero orphans)
        Integer orphanProfiles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM intern_profiles ip LEFT JOIN users u ON ip.user_id = u.id WHERE u.id IS NULL", Integer.class);
        Integer orphanReports = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM weekly_reports wr LEFT JOIN intern_profiles ip ON wr.intern_id = ip.id WHERE ip.id IS NULL", Integer.class);

        assertEquals(0, orphanProfiles, "Zero orphan intern profiles allowed");
        assertEquals(0, orphanReports, "Zero orphan weekly reports allowed");
    }

    @Test
    @DisplayName("Capture Raw EXPLAIN ANALYZE for Keyword Search Query")
    void captureExplainAnalyzeForSearchQuery() {
        String sql = "EXPLAIN ANALYZE SELECT ip.*, u.full_name, u.email " +
                "FROM intern_profiles ip " +
                "JOIN users u ON ip.user_id = u.id " +
                "WHERE LOWER(u.full_name) LIKE '%nguyen%' OR LOWER(ip.student_code) LIKE '%nguyen%'";

        List<Map<String, Object>> explainResult = jdbcTemplate.queryForList(sql);
        assertNotNull(explainResult);
        assertFalse(explainResult.isEmpty());

        System.out.println("=== RAW EXPLAIN ANALYZE OUTPUT FOR SEARCH ===");
        explainResult.forEach(row -> System.out.println(row.values()));
        System.out.println("============================================");
    }
}
