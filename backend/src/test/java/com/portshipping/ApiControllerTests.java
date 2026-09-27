package com.portshipping;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsDatabaseStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidVesselPayloadReturnsValidationDetails() throws Exception {
        mockMvc.perform(post("/api/vessels").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"imoNumber\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validation.name").exists())
                .andExpect(jsonPath("$.validation.imoNumber").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void missingVesselReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/vessels/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/vessels/999999"));
    }

    @Test
    void unauthenticatedUserCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/vessels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerRoleCannotCreateVessel() throws Exception {
        mockMvc.perform(post("/api/vessels").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Vessel\",\"imoNumber\":\"IMO1234567\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRegistrationAndLoginWorkflow() throws Exception {
        String registrationPayload = "{\"name\":\"Test User\",\"email\":\"newuser@demo.invalid\",\"password\":\"password123\"}";
        
        // 1. Successful Registration -> 201 Created & default VIEWER role assigned
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@demo.invalid"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // 2. Duplicate Registration -> 409 Conflict
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationPayload))
                .andExpect(status().isConflict());

        // 3. Login with Invalid Password -> 401 Unauthorized
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@demo.invalid\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());

        // 4. Login with Correct Credentials -> 200 OK
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@demo.invalid\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newuser@demo.invalid"));
    }
}
