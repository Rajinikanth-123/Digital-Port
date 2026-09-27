package com.portshipping;

import com.portshipping.dto.*;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import com.portshipping.service.InvoiceService;
import com.portshipping.service.PaymentService;
import com.portshipping.service.ReportsService;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ReportsAnalyticsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private VesselRepository vesselRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private CustomsClearanceRepository customsClearanceRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReportsService reportsService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        customsClearanceRepository.deleteAll();
        containerRepository.deleteAll();
        shipmentRepository.deleteAll();
        vesselRepository.deleteAll();

        adminUser = userRepository.findByEmailIgnoreCase("admin@portshipping.com").orElseGet(() -> {
            User u = new User("Reports Admin", "admin@portshipping.com", "pass");
            u.setRole("ADMIN");
            return userRepository.save(u);
        });
    }

    @Test
    void testGetOperationalSummary_CalculatesCorrectTotals() {
        Vessel v = vesselRepository.save(new Vessel("Titan Explorer", "IMO-998877", "CONTAINER", "Panama", 10000, "Global Lines", 300.0, 14.0, "IN_PORT"));

        Shipment s1 = shipmentRepository.save(new Shipment("SH-9001", "REF-SH-9001", "Port A", "Port B", "Shipper A", "Consignee B", null, "IN_TRANSIT", 2, v));
        Shipment s2 = shipmentRepository.save(new Shipment("SH-9002", "REF-SH-9002", "Port A", "Port C", "Shipper A", "Consignee C", null, "DELIVERED", 3, v));

        containerRepository.save(new Container("CONT-888", "40FT", "DRY", BigDecimal.ZERO, "Yard A", "CUSTOMS_HOLD", s1));

        customsClearanceRepository.save(new CustomsClearance(s1, "DEC-777", "PENDING", "Declaration details"));

        OperationalSummaryDTO summary = reportsService.getOperationalSummary(null, null);

        assertEquals(1, summary.getTotalVessels());
        assertEquals(1, summary.getVesselsInPort());
        assertEquals(2, summary.getTotalShipments());
        assertEquals(1, summary.getActiveShipments());
        assertEquals(1, summary.getDeliveredShipments());
        assertEquals(1, summary.getTotalContainers());
        assertEquals(1, summary.getContainersInHold());
        assertEquals(1, summary.getPendingCustoms());
    }

    @Test
    void testGetShipmentStatusDistribution_GroupsByStatus() {
        Vessel v = vesselRepository.save(new Vessel("Maersk Titan", "IMO-112233", "CONTAINER", "Liberia", 12000, "Maersk", 320.0, 15.0, "IN_PORT"));

        shipmentRepository.save(new Shipment("SH-DIST-1", "REF-SH-D1", "Origin 1", "Dest 1", "Shipper 1", "Consignee 1", null, "IN_TRANSIT", 1, v));
        shipmentRepository.save(new Shipment("SH-DIST-2", "REF-SH-D2", "Origin 2", "Dest 2", "Shipper 2", "Consignee 2", null, "IN_TRANSIT", 2, v));
        shipmentRepository.save(new Shipment("SH-DIST-3", "REF-SH-D3", "Origin 3", "Dest 3", "Shipper 3", "Consignee 3", null, "DELIVERED", 1, v));

        List<StatusDistributionDTO> list = reportsService.getShipmentStatusDistribution(null, null);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertTrue(list.stream().anyMatch(d -> "IN_TRANSIT".equals(d.getStatus()) && d.getCount() == 2));
        assertTrue(list.stream().anyMatch(d -> "DELIVERED".equals(d.getStatus()) && d.getCount() == 1));
    }

    @Test
    void testGetFinancialSummaryAndRevenueTrends_CalculatesAmounts() {
        Invoice inv = invoiceService.createInvoice(Map.of(
                "invoiceNumber", "INV-REP-01",
                "customerName", "Global Logistics",
                "charges", 5000.00,
                "taxes", 500.00,
                "discount", 0.00,
                "dueDate", LocalDate.now().plusDays(10).toString()
        ), adminUser);

        paymentService.recordPayment(Map.of(
                "invoiceId", inv.getId(),
                "amount", 2500.00,
                "reference", "PAY-REP-01",
                "paymentMethod", "WIRE_TRANSFER"
        ), adminUser);

        FinancialAnalyticsDTO fin = reportsService.getFinancialSummary(null, null);

        assertEquals(new BigDecimal("5500.00"), fin.getTotalInvoiced());
        assertEquals(new BigDecimal("2500.00"), fin.getTotalCollected());
        assertEquals(new BigDecimal("3000.00"), fin.getOutstandingBalance());
        assertEquals(1, fin.getTotalInvoiceCount());

        List<TimeSeriesPointDTO> trends = reportsService.getRevenueTrends(null, null, "DAILY");
        assertNotNull(trends);
        assertFalse(trends.isEmpty());
        assertTrue(trends.get(0).getAmount().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testInvalidDateRange_ThrowsException() {
        assertThrows(ResponseStatusException.class, () ->
                reportsService.getOperationalSummary("2026-10-10", "2026-01-01"));
    }

    @Test
    void testCsvFormulaInjectionSanitization() {
        String dangerousValue = "=SUM(A1:A100)";
        String sanitized = reportsService.sanitizeCsvCell(dangerousValue);

        assertTrue(sanitized.contains("'=SUM(A1:A100)"));
    }

    @Test
    @WithMockUser(username = "admin@portshipping.com", roles = {"ADMIN"})
    void testExportCsvAndPdfEndpoints_AuthorizedUser_Returns200OK() throws Exception {
        mockMvc.perform(get("/api/reports/export/csv?reportType=financial"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));

        mockMvc.perform(get("/api/reports/export/pdf?reportType=summary"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @WithMockUser(username = "viewer@portshipping.com", roles = {"VIEWER"})
    void testFinancialEndpoints_ForbiddenForViewerRole() throws Exception {
        mockMvc.perform(get("/api/reports/financial-summary"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/revenue-trends"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/export/csv?reportType=financial"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "viewer@portshipping.com", roles = {"VIEWER"})
    void testOperationalEndpoints_AllowedForViewerRole() throws Exception {
        mockMvc.perform(get("/api/reports/operational-summary"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reports/shipment-distribution"))
                .andExpect(status().isOk());
    }
}
