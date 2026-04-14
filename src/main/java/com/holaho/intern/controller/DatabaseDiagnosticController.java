package com.holaho.intern.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal/diag")
@RequiredArgsConstructor
public class DatabaseDiagnosticController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/schema")
    public List<Map<String, Object>> getTableSchema(@RequestParam String table) {
        return jdbcTemplate.queryForList("DESCRIBE " + table);
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> listTables() {
        return jdbcTemplate.queryForList("SHOW TABLES");
    }

    @GetMapping("/flyway")
    public List<Map<String, Object>> getFlywayHistory() {
        return jdbcTemplate.queryForList("SELECT * FROM flyway_schema_history");
    }

    @GetMapping("/repair-flyway")
    public String repairFlyway() {
        int rows = jdbcTemplate.update("DELETE FROM flyway_schema_history WHERE version = '10' AND success = 0");
        return "Deleted " + rows + " failed migration rows for version 10. You can now restart the server.";
    }
}

