package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
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
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PortTerminalVesselBerthManagementTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PortRepository portRepository;
    @Autowired private TerminalRepository terminalRepository;
    @Autowired private VesselRepository vesselRepository;
    @Autowired private BerthRepository berthRepository;
    @Autowired private BerthAllocationRepository allocationRepository;

    private Port testPort;
    private Terminal testTerminal;
    private Vessel testVessel;
    private Berth testBerth;

    @BeforeEach
    void setUp() {
        allocationRepository.deleteAll();
        berthRepository.deleteAll();
        terminalRepository.deleteAll();
        portRepository.deleteAll();
        vesselRepository.deleteAll();

        testPort = portRepository.save(new Port("Port of Hamburg", "DEHAM", "Germany", "Europe/Berlin", 53.5511, 9.9937));
        testTerminal = terminalRepository.save(new Terminal(testPort, "Altenwerder Terminal", "CTA-HAM", "CONTAINER", 150000));
        testVessel = vesselRepository.save(new Vessel("Ever Given", "IMO9811000", "CONTAINER", "Panama", 20000, "Shoei Kisen", 400.0, 16.0, "ACTIVE"));
        testBerth = berthRepository.save(new Berth(testTerminal, "Berth 1", 400.0, "AVAILABLE"));
    }

    // ==========================================
    // 1. PORT MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "admin@port.com", roles = {"ADMIN"})
    void testCreatePort_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Port of Singapore",
                "code", "SGSIN",
                "country", "Singapore",
                "timezone", "Asia/Singapore",
                "latitude", 1.290270,
                "longitude", 103.851959
        );

        mockMvc.perform(post("/api/ports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Port of Singapore")))
                .andExpect(jsonPath("$.code", is("SGSIN")));
    }

    @Test
    @WithMockUser(username = "admin@port.com", roles = {"ADMIN"})
    void testCreatePort_DuplicateCode_Returns409Conflict() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Hamburg Duplicate",
                "code", "DEHAM",
                "country", "Germany"
        );

        mockMvc.perform(post("/api/ports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("DEHAM")));
    }

    @Test
    @WithMockUser(username = "admin@port.com", roles = {"ADMIN"})
    void testDeletePort_ReferencedByTerminal_Returns409Conflict() throws Exception {
        mockMvc.perform(delete("/api/ports/" + testPort.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("referenced by")));
    }

    // ==========================================
    // 2. TERMINAL MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testCreateTerminal_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Eurogate Terminal",
                "code", "EG-HAM",
                "portId", testPort.getId(),
                "terminalType", "CONTAINER",
                "capacity", 200000
        );

        mockMvc.perform(post("/api/terminals")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Eurogate Terminal")))
                .andExpect(jsonPath("$.code", is("EG-HAM")));
    }

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testDeleteTerminal_ReferencedByBerth_Returns409Conflict() throws Exception {
        mockMvc.perform(delete("/api/terminals/" + testTerminal.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("referenced by")));
    }

    // ==========================================
    // 3. VESSEL MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateVessel_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "CMA CGM Antoine",
                "code", "IMO9454436",
                "imoNumber", "IMO9454436",
                "vesselType", "CONTAINER",
                "flag", "France",
                "capacity", 16000,
                "operatorName", "CMA CGM",
                "length", 396.0,
                "draft", 15.5,
                "status", "ACTIVE"
        );

        mockMvc.perform(post("/api/vessels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imoNumber", is("IMO9454436")));
    }

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateVessel_DuplicateIMO_Returns409Conflict() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Ever Given Duplicate",
                "imoNumber", "IMO9811000",
                "vesselType", "CONTAINER"
        );

        mockMvc.perform(post("/api/vessels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("IMO9811000")));
    }

    // ==========================================
    // 4. BERTH MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "admin@port.com", roles = {"ADMIN"})
    void testCreateBerth_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Berth 2",
                "terminalId", testTerminal.getId(),
                "maxLengthM", 380.0,
                "status", "AVAILABLE"
        );

        mockMvc.perform(post("/api/berths")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Berth 2")));
    }

    // ==========================================
    // 5. BERTH ALLOCATION OVERLAP TESTS (409 CONFLICT)
    // ==========================================

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testCreateBerthAllocation_ConflictDetection_Returns409() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant start1 = now.plus(1, ChronoUnit.HOURS);
        Instant end1 = now.plus(5, ChronoUnit.HOURS);

        // 1. Create first allocation
        Map<String, Object> req1 = Map.of(
                "berthId", testBerth.getId(),
                "berth", testBerth.getName(),
                "vesselId", testVessel.getId(),
                "startTime", start1.toString(),
                "endTime", end1.toString()
        );

        mockMvc.perform(post("/api/berth-allocations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // 2. Create overlapping allocation for same berth (3 hours to 7 hours)
        Instant start2 = now.plus(3, ChronoUnit.HOURS);
        Instant end2 = now.plus(7, ChronoUnit.HOURS);

        Map<String, Object> req2 = Map.of(
                "berthId", testBerth.getId(),
                "berth", testBerth.getName(),
                "vesselId", testVessel.getId(),
                "startTime", start2.toString(),
                "endTime", end2.toString()
        );

        mockMvc.perform(post("/api/berth-allocations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("conflict")));
    }

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testCreateBerthAllocation_InvalidTimeRange_Returns400() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant start = now.plus(5, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS); // End before start

        Map<String, Object> req = Map.of(
                "berthId", testBerth.getId(),
                "berth", testBerth.getName(),
                "vesselId", testVessel.getId(),
                "startTime", start.toString(),
                "endTime", end.toString()
        );

        mockMvc.perform(post("/api/berth-allocations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("before")));
    }

    // ==========================================
    // 6. ROLE AUTHORIZATION TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "viewer@port.com", roles = {"VIEWER"})
    void testViewerRole_CannotCreatePort_Returns403Forbidden() throws Exception {
        Map<String, Object> req = Map.of(
                "name", "Unauthorized Port",
                "code", "UNAUTH",
                "country", "Test"
        );

        mockMvc.perform(post("/api/ports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticated_CannotAccessApi_Returns401Or403() throws Exception {
        mockMvc.perform(post("/api/ports")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
