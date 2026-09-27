package com.portshipping;

import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class GlobalSearchSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private VesselRepository vesselRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private PortRepository portRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        containerRepository.deleteAll();
        shipmentRepository.deleteAll();
        vesselRepository.deleteAll();
        portRepository.deleteAll();

        adminUser = userRepository.findByEmailIgnoreCase("admin_search@portshipping.com").orElseGet(() -> {
            User u = new User("Search Admin", "admin_search@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setRole("ADMIN");
            return userRepository.save(u);
        });

        viewerUser = userRepository.findByEmailIgnoreCase("viewer_search@portshipping.com").orElseGet(() -> {
            User u = new User("Search Viewer", "viewer_search@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setRole("VIEWER");
            return userRepository.save(u);
        });

        // Seed domain test data
        Port port = new Port("Pacific Gateway Port", "PGP-01", "USA");
        portRepository.save(port);

        Vessel vessel = new Vessel("Pacific Trader", "IMO9876543", "Container Ship", "Panama", 5000, "Global Shipping Co.", 250.0, 12.0, "ACTIVE");
        vesselRepository.save(vessel);

        Shipment shipment = new Shipment("TRK-PACIFIC-100", "REF-100", "Pacific Gateway Port", "Tokyo Port", "Pacific Logistics", "Nippon Imports", LocalDate.now().plusDays(10), "IN_TRANSIT", 2, vessel);
        shipmentRepository.save(shipment);

        Container container = new Container("CONT-PACIFIC-55", "40FT", "DRY", new BigDecimal("12000"), "Pacific Gateway Port", "REGISTERED", shipment);
        containerRepository.save(container);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-PACIFIC-999");
        invoice.setCustomerName("Pacific Logistics");
        invoice.setPaymentStatus("ISSUED");
        invoice.setCurrency("USD");
        invoice.setCharges(new BigDecimal("15000.00"));
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoiceRepository.save(invoice);
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testGlobalSearchAcrossMultipleModules() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "Pacific"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$.items[*].module", hasItems("SHIPMENT", "VESSEL", "PORT", "INVOICE")));
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testFilterByModuleAndStatus() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "Pacific")
                        .param("module", "SHIPMENT")
                        .param("status", "IN_TRANSIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.items[0].module", is("SHIPMENT")))
                .andExpect(jsonPath("$.items[0].referenceNumber", is("TRK-PACIFIC-100")))
                .andExpect(jsonPath("$.items[0].status", is("IN_TRANSIT")));
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testDateRangeValidation() throws Exception {
        // Valid date range
        mockMvc.perform(get("/api/search")
                        .param("startDate", LocalDate.now().minusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk());

        // Invalid date range (startDate after endDate)
        mockMvc.perform(get("/api/search")
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Start date cannot be after end date")));
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testQueryLengthLimit() throws Exception {
        String longQuery = "a".repeat(101);
        mockMvc.perform(get("/api/search")
                        .param("q", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("maximum allowed length")));
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testSearchSuggestionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/search/suggestions")
                        .param("q", "Paci")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].displayTitle", notNullValue()));
    }

    @Test
    @WithMockUser(username = "viewer_search@portshipping.com", roles = {"VIEWER"})
    void testRoleBasedAccessControlRestrictions() throws Exception {
        // VIEWER role is restricted from searching INVOICES or USERS
        mockMvc.perform(get("/api/search")
                        .param("q", "Pacific"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].module", not(hasItem("INVOICE"))))
                .andExpect(jsonPath("$.items[*].module", not(hasItem("USER"))));
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testSqlInjectionAndWildcardSafety() throws Exception {
        // SQL injection payload should be handled safely as literal search text
        mockMvc.perform(get("/api/search")
                        .param("q", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        // SQL wildcard characters (% and _)
        mockMvc.perform(get("/api/search")
                        .param("q", "%_"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_search@portshipping.com", roles = {"ADMIN"})
    void testPaginationAndDeterministicSorting() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "Pacific")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "title")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)));
    }
}
