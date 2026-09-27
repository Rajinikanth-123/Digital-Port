package com.portshipping;

import com.portshipping.entity.AuditLog;
import com.portshipping.entity.User;
import com.portshipping.repository.AuditLogRepository;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuditLogSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        adminUser = userRepository.findByEmailIgnoreCase("admin_audit@portshipping.com").orElseGet(() -> {
            User u = new User("Audit Admin", "admin_audit@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setRole("ADMIN");
            return userRepository.save(u);
        });

        normalUser = userRepository.findByEmailIgnoreCase("user_audit@portshipping.com").orElseGet(() -> {
            User u = new User("Audit User", "user_audit@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setRole("VIEWER");
            return userRepository.save(u);
        });
    }

    @Test
    void testAuditRecordCreationAndActorAttribution() {
        AuditLog log = auditLogService.logSuccess(adminUser, "VESSEL_CREATED", "Vessel", 101L, "Created vessel IMO-9876543");

        assertNotNull(log);
        assertNotNull(log.getId());
        assertEquals("VESSEL_CREATED", log.getAction());
        assertEquals("Vessel", log.getEntityName());
        assertEquals(101L, log.getEntityId());
        assertEquals("SUCCESS", log.getOutcome());
        assertEquals("Audit Admin", log.getActorName());
        assertEquals("ADMIN", log.getActorRole());
    }

    @Test
    void testFieldLevelChangeTrackingAndDiffJson() {
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("status", "REGISTERED");
        oldValues.put("containerCount", 5);
        oldValues.put("password", "Secret123!");

        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("status", "IN_TRANSIT");
        newValues.put("containerCount", 8);
        newValues.put("password", "NewSecret456!");

        String diffJson = auditLogService.buildFieldDiffJson(oldValues, newValues);

        assertNotNull(diffJson);
        assertTrue(diffJson.contains("REGISTERED"));
        assertTrue(diffJson.contains("IN_TRANSIT"));
        // Sensitive password field must be omitted from change tracking
        assertFalse(diffJson.contains("Secret123!"));
        assertFalse(diffJson.contains("NewSecret456!"));
    }

    @Test
    void testSensitiveFieldRedaction() {
        String jsonWithSecret = "{\"username\":\"admin\",\"password\":\"supersecret123\",\"token\":\"abc123xyz\"}";
        String redacted = auditLogService.redactSensitiveJson(jsonWithSecret);

        assertNotNull(redacted);
        assertFalse(redacted.contains("supersecret123"));
        assertTrue(redacted.contains("[REDACTED]"));
    }

    @Test
    void testLoginSuccessAndFailureRecording() throws Exception {
        // Successful login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin_audit@portshipping.com\",\"password\":\"Password123!\"}")
                        .with(csrf()))
                .andExpect(status().isOk());

        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "LOGIN_SUCCESS".equals(l.getAction())));

        // Failed login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin_audit@portshipping.com\",\"password\":\"WrongPass!\"}")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());

        assertTrue(auditLogRepository.findAll().stream().anyMatch(l -> "LOGIN_FAILED".equals(l.getAction()) && "FAILURE".equals(l.getOutcome())));
    }

    @Test
    @WithMockUser(username = "admin_audit@portshipping.com", roles = {"ADMIN"})
    void testAuditLogApiPaginationAndFiltering() throws Exception {
        auditLogService.logSuccess(adminUser, "USER_CREATED", "User", 1L, "Created user 1");
        auditLogService.logFailure(adminUser, "LOGIN_FAILED", "Auth", null, "Bad password");
        auditLogService.logSuccess(adminUser, "INVOICE_ISSUED", "Invoice", 20L, "Issued invoice #INV-90");

        // Filter by outcome SUCCESS
        mockMvc.perform(get("/api/audit-logs?outcome=SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // Filter by module User
        mockMvc.perform(get("/api/audit-logs?entityName=User"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("USER_CREATED"));

        // Summary endpoint
        mockMvc.perform(get("/api/audit-logs/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(1));
    }

    @Test
    @WithMockUser(username = "admin_audit@portshipping.com", roles = {"ADMIN"})
    void testInvalidDateRange_ReturnsBadRequest() throws Exception {
        String startDate = Instant.now().toString();
        String endDate = Instant.now().minus(2, ChronoUnit.DAYS).toString();

        mockMvc.perform(get("/api/audit-logs?startDate=" + startDate + "&endDate=" + endDate))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user_audit@portshipping.com", roles = {"VIEWER"})
    void testNonAdminAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-logs/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_audit@portshipping.com", roles = {"ADMIN"})
    void testAuditLogImmutability_NoModificationEndpointsExist() throws Exception {
        AuditLog log = auditLogService.logSuccess(adminUser, "TEST_ACTION", "Test", 99L, "Immutable record");

        // PUT/POST/DELETE must return 405 Method Not Allowed or 404
        mockMvc.perform(put("/api/audit-logs/" + log.getId()).with(csrf()))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete("/api/audit-logs/" + log.getId()).with(csrf()))
                .andExpect(status().is4xxClientError());
    }
}
