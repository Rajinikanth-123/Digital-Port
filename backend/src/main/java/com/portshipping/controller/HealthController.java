package com.portshipping.controller;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final JdbcTemplate jdbcTemplate;

    public HealthController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @GetMapping
    public Map<String, Object> health() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of("status", "UP", "database", result != null && result == 1 ? "UP" : "DOWN",
                    "timestamp", Instant.now());
        } catch (RuntimeException exception) {
            return Map.of("status", "DEGRADED", "database", "DOWN", "timestamp", Instant.now());
        }
    }
}
