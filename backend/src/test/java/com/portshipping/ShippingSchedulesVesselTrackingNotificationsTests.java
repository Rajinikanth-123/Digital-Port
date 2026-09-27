package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import com.portshipping.service.NotificationService;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ShippingSchedulesVesselTrackingNotificationsTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VesselRepository vesselRepository;
    @Autowired private PortRepository portRepository;
    @Autowired private TerminalRepository terminalRepository;
    @Autowired private BerthRepository berthRepository;
    @Autowired private ShippingScheduleRepository scheduleRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    private Vessel testVessel;
    private Berth testBerth;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmailIgnoreCase("admin@port.local")
                .orElseGet(() -> {
                    User u = new User("Admin Test", "admin@port.local", "encodedpass");
                    u.setRole("ADMIN");
                    return userRepository.save(u);
                });

        testVessel = vesselRepository.save(new Vessel(
                "MV Atlantic Star", "IMO9988771", "CONTAINER", "Liberia", 14000, "OceanLine", 366.0, 15.2, "ACTIVE"
        ));

        Port testPort = portRepository.save(new Port("Test Port Alpha", "TPA01", "Netherlands"));
        Terminal testTerminal = terminalRepository.save(new Terminal(testPort, "Terminal 1", "T101"));
        testBerth = berthRepository.save(new Berth(testTerminal, "Berth Test-101", 400.0, "AVAILABLE"));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testCreateScheduleSuccess() throws Exception {
        LocalDateTime eta = LocalDateTime.now().plusDays(2);
        LocalDateTime etd = eta.plusHours(14);

        Map<String, Object> req = Map.of(
                "vesselId", testVessel.getId(),
                "berthId", testBerth.getId(),
                "arrivalEstimated", eta.toString(),
                "departureEstimated", etd.toString(),
                "status", "SCHEDULED"
        );

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.vessel.id", is(testVessel.getId().intValue())))
                .andExpect(jsonPath("$.status", is("SCHEDULED")));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testCreateScheduleInvalidDatesReturnsBadRequest() throws Exception {
        LocalDateTime eta = LocalDateTime.now().plusDays(2);
        LocalDateTime invalidEtd = eta.minusHours(2);

        Map<String, Object> req = Map.of(
                "vesselId", testVessel.getId(),
                "berthId", testBerth.getId(),
                "arrivalEstimated", eta.toString(),
                "departureEstimated", invalidEtd.toString(),
                "status", "SCHEDULED"
        );

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testScheduleOverlapConflictReturns409() throws Exception {
        LocalDateTime eta = LocalDateTime.now().plusDays(5);
        LocalDateTime etd = eta.plusHours(20);

        scheduleRepository.save(new ShippingSchedule(testVessel, testBerth, eta, etd, "SCHEDULED"));

        Map<String, Object> req = Map.of(
                "vesselId", testVessel.getId(),
                "berthId", testBerth.getId(),
                "arrivalEstimated", eta.plusHours(2).toString(),
                "departureEstimated", etd.plusHours(2).toString(),
                "status", "SCHEDULED"
        );

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testVesselPositionTrackingUpdate() throws Exception {
        Map<String, Object> req = Map.of(
                "latitude", 51.9546,
                "longitude", 4.0205,
                "currentLocation", "Rotterdam Approach Channel",
                "status", "IN_TRANSIT"
        );

        mockMvc.perform(post("/api/vessels/" + testVessel.getId() + "/tracking")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude", is(51.9546)))
                .andExpect(jsonPath("$.longitude", is(4.0205)))
                .andExpect(jsonPath("$.currentLocation", is("Rotterdam Approach Channel")))
                .andExpect(jsonPath("$.status", is("IN_TRANSIT")));

        mockMvc.perform(get("/api/vessels/" + testVessel.getId() + "/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testVesselTrackingInvalidCoordinatesReturnsBadRequest() throws Exception {
        Map<String, Object> req = Map.of(
                "latitude", 95.0,
                "longitude", 4.0205,
                "currentLocation", "Invalid Position",
                "status", "IN_TRANSIT"
        );

        mockMvc.perform(post("/api/vessels/" + testVessel.getId() + "/tracking")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@port.local", roles = {"ADMIN"})
    void testNotificationsLifecycleAndDeduplication() throws Exception {
        Notification n1 = notificationService.sendNotification(testUser, "Test Alert", "Operational status update.", "VESSEL", "Vessel", testVessel.getId());
        assertNotNull(n1);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/notifications/" + n1.getId() + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus", is(true)));

        Notification dup = notificationService.sendNotification(testUser, "Test Alert", "Operational status update.", "VESSEL", "Vessel", testVessel.getId());
        assertNull(dup, "Duplicate notification within 30 seconds should be suppressed");
    }

    @Test
    @WithMockUser(username = "viewer@port.local", roles = {"VIEWER"})
    void testViewerRoleCannotCreateSchedule() throws Exception {
        Map<String, Object> req = Map.of(
                "vesselId", testVessel.getId(),
                "arrivalEstimated", LocalDateTime.now().plusDays(1).toString(),
                "status", "SCHEDULED"
        );

        mockMvc.perform(post("/api/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
