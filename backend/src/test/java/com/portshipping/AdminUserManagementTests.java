package com.portshipping;

import com.portshipping.entity.User;
import com.portshipping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User primaryAdmin;

    @BeforeEach
    void setUp() {
        // Ensure at least one active admin exists for tests
        primaryAdmin = userRepository.findByEmailIgnoreCase("admin@demo.invalid").orElseGet(() -> {
            User u = new User("System Admin", "admin@demo.invalid", passwordEncoder.encode("admin12345"));
            u.setRole("ADMIN");
            u.setEnabled(true);
            return userRepository.save(u);
        });
        if (!primaryAdmin.isEnabled() || !"ADMIN".equalsIgnoreCase(primaryAdmin.getRole())) {
            primaryAdmin.setRole("ADMIN");
            primaryAdmin.setEnabled(true);
            userRepository.save(primaryAdmin);
        }
    }

    @Test
    void unauthenticatedUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"test@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void nonAdminRoleIsForbiddenFromUserManagement() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"email\":\"test@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessDashboardUsersAndAuditLogs() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeUsers").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.vessels").exists())
                .andExpect(jsonPath("$.shipments").exists());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUserLifecycleAndProtectionTest() throws Exception {
        // 1. Create new user
        String createPayload = """
                {
                    "name": "Operator Demo",
                    "fullName": "Operator Full Demo",
                    "email": "operator.demo@portshipping.com",
                    "password": "Password123!",
                    "role": "PORT_MANAGER",
                    "enabled": true
                }
                """;

        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("operator.demo@portshipping.com"))
                .andExpect(jsonPath("$.role").value("PORT_MANAGER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Long createdUserId = userRepository.findByEmailIgnoreCase("operator.demo@portshipping.com").get().getId();

        // 2. Duplicate Email Registration Conflict
        mockMvc.perform(post("/api/users").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isConflict());

        // 3. Update User details
        mockMvc.perform(put("/api/users/" + createdUserId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Operator Updated\",\"email\":\"operator.demo@portshipping.com\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Operator Updated"))
                .andExpect(jsonPath("$.enabled").value(false));

        // 4. Update Role to VIEWER
        mockMvc.perform(put("/api/users/" + createdUserId + "/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        // 5. Invalid Role update returns 400 Bad Request
        mockMvc.perform(put("/api/users/" + createdUserId + "/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isBadRequest());

        // 6. Delete created user
        mockMvc.perform(delete("/api/users/" + createdUserId).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void preventsDisablingDemotingOrDeletingFinalActiveAdmin() throws Exception {
        // Make sure primaryAdmin is the ONLY active admin for this test
        userRepository.findAll().forEach(u -> {
            if (!u.getId().equals(primaryAdmin.getId()) && "ADMIN".equalsIgnoreCase(u.getRole())) {
                u.setRole("VIEWER");
                userRepository.save(u);
            }
        });

        // 1. Attempt to deactivate the last admin -> 409 Conflict
        mockMvc.perform(put("/api/users/" + primaryAdmin.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot deactivate the final active administrator."));

        // 2. Attempt to demote the last admin role -> 409 Conflict
        mockMvc.perform(put("/api/users/" + primaryAdmin.getId() + "/role").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot remove administrative role from the final active administrator."));

        // 3. Attempt to delete the last admin -> 409 Conflict
        mockMvc.perform(delete("/api/users/" + primaryAdmin.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete the final active administrator."));
    }
}
