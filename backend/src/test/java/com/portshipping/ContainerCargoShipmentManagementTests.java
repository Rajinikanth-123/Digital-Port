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

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ContainerCargoShipmentManagementTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerRepository containerRepository;
    @Autowired private CargoRepository cargoRepository;
    @Autowired private ShipmentRepository shipmentRepository;
    @Autowired private VesselRepository vesselRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private Shipment testShipment;
    private Vessel testVessel;

    @BeforeEach
    void setUp() {
        containerRepository.deleteAll();
        cargoRepository.deleteAll();
        shipmentRepository.deleteAll();
        vesselRepository.deleteAll();
        auditLogRepository.deleteAll();

        testVessel = vesselRepository.save(new Vessel("MAERSK MC-KINNEY", "IMO9632064", "CONTAINER", "Denmark", 18270, "Maersk Line", 399.0, 16.0, "ACTIVE"));
        testShipment = shipmentRepository.save(new Shipment("TRK-1001", "REF-SH-1001", "Shanghai Port", "Rotterdam Port", "Global Shipper Inc", "Euro Cargo BV", null, "REGISTERED", 2, testVessel));
    }

    // ==========================================
    // 1. CONTAINER MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateContainer_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "containerNumber", "MSKU9876543",
                "size", "40HC",
                "type", "REEFER",
                "weightKg", 12500.5,
                "location", "Yard B-04",
                "status", "AVAILABLE",
                "shipmentId", testShipment.getId()
        );

        mockMvc.perform(post("/api/containers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.containerNumber", is("MSKU9876543")))
                .andExpect(jsonPath("$.size", is("40HC")))
                .andExpect(jsonPath("$.type", is("REEFER")));
    }

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateContainer_DuplicateNumber_Returns409Conflict() throws Exception {
        containerRepository.save(new Container("MSKU9876543", "40FT", "DRY", BigDecimal.ZERO, "Yard A", "AVAILABLE", null));

        Map<String, Object> req = Map.of(
                "containerNumber", "MSKU9876543",
                "size", "40FT"
        );

        mockMvc.perform(post("/api/containers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("MSKU9876543")));
    }

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateContainer_NegativeWeight_Returns400BadRequest() throws Exception {
        Map<String, Object> req = Map.of(
                "containerNumber", "MSKU1111222",
                "weightKg", -50.0
        );

        mockMvc.perform(post("/api/containers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 2. CARGO MANAGEMENT TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testCreateCargo_Success() throws Exception {
        Map<String, Object> req = Map.of(
                "description", "Precision Automotive Engines",
                "cargoType", "AUTOMOTIVE",
                "quantity", 5,
                "weightKg", 8500.0,
                "shipmentId", testShipment.getId()
        );

        mockMvc.perform(post("/api/cargo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description", is("Precision Automotive Engines")))
                .andExpect(jsonPath("$.quantity", is(5)));
    }

    @Test
    @WithMockUser(username = "manager@port.com", roles = {"PORT_MANAGER"})
    void testCreateCargo_InvalidShipmentId_Returns400BadRequest() throws Exception {
        Map<String, Object> req = Map.of(
                "description", "Invalid Cargo",
                "shipmentId", 99999L
        );

        mockMvc.perform(post("/api/cargo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid shipment ID")));
    }

    // ==========================================
    // 3. SHIPMENT & TRACKING TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testCreateShipment_GeneratesTrackingHistory() throws Exception {
        Map<String, Object> req = Map.of(
                "referenceNumber", "REF-SHIP-2026-99",
                "origin", "Port of Busan",
                "destination", "Port of Los Angeles",
                "status", "REGISTERED",
                "containerCount", 4,
                "vesselId", testVessel.getId()
        );

        String responseStr = mockMvc.perform(post("/api/shipments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.referenceNumber", is("REF-SHIP-2026-99")))
                .andReturn().getResponse().getContentAsString();

        Long createdId = mapper.readTree(responseStr).get("id").asLong();

        // Verify tracking timeline endpoint returns creation event
        mockMvc.perform(get("/api/shipments/" + createdId + "/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].action", is("SHIPMENT_CREATED")));
    }

    @Test
    @WithMockUser(username = "agent@port.com", roles = {"SHIPPING_AGENT"})
    void testUpdateShipmentStatus_LogsTrackingEvent() throws Exception {
        Map<String, Object> req = Map.of(
                "referenceNumber", testShipment.getReferenceNumber(),
                "origin", testShipment.getOrigin(),
                "destination", testShipment.getDestination(),
                "status", "IN_TRANSIT"
        );

        mockMvc.perform(put("/api/shipments/" + testShipment.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));

        mockMvc.perform(get("/api/shipments/" + testShipment.getId() + "/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action", is("SHIPMENT_STATUS_UPDATED")));
    }

    @Test
    @WithMockUser(username = "admin@port.com", roles = {"ADMIN"})
    void testDeleteShipment_LinkedToContainer_Returns409Conflict() throws Exception {
        containerRepository.save(new Container("MSKU0000001", "40FT", "DRY", BigDecimal.ZERO, "Yard", "LOADED", testShipment));

        mockMvc.perform(delete("/api/shipments/" + testShipment.getId())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("linked to")));
    }

    // ==========================================
    // 4. ROLE AUTHORIZATION TESTS
    // ==========================================

    @Test
    @WithMockUser(username = "viewer@port.com", roles = {"VIEWER"})
    void testViewerRole_CannotCreateContainer_Returns403Forbidden() throws Exception {
        Map<String, Object> req = Map.of(
                "containerNumber", "MSKU7777777",
                "size", "20FT"
        );

        mockMvc.perform(post("/api/containers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticated_CannotAccessContainerApi_Returns401Or403() throws Exception {
        mockMvc.perform(post("/api/containers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
