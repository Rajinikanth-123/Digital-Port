package com.portshipping;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
class DigitalPortShippingApplicationTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void databaseIsAccessible() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, result);
    }
}
