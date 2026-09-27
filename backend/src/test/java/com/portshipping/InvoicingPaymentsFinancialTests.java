package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import com.portshipping.service.FinancialDashboardService;
import com.portshipping.service.InvoiceService;
import com.portshipping.service.PaymentService;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvoicingPaymentsFinancialTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FinancialDashboardService financialDashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();

        adminUser = userRepository.findByEmailIgnoreCase("admin@portshipping.com").orElseGet(() -> {
            User u = new User("Financial Admin", "admin@portshipping.com", "hashedpass");
            u.setRole("ADMIN");
            return userRepository.save(u);
        });
    }

    @Test
    void testCreateInvoice_WithLineItems_CalculatesTotal() {
        Map<String, Object> req = new HashMap<>();
        req.put("customerName", "Global Freight Lines");
        req.put("currency", "USD");
        req.put("taxes", 100.00);
        req.put("discount", 50.00);
        req.put("dueDate", LocalDate.now().plusDays(15).toString());

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of("description", "Container Discharge", "quantity", 2, "unitPrice", 500.00));
        items.add(Map.of("description", "Storage Fee", "quantity", 5, "unitPrice", 100.00));
        req.put("items", items);

        Invoice created = invoiceService.createInvoice(req, adminUser);

        assertNotNull(created.getId());
        assertNotNull(created.getInvoiceNumber());
        assertEquals("Global Freight Lines", created.getCustomerName());

        // Charges = (2 * 500) + (5 * 100) = 1500.00
        assertEquals(new BigDecimal("1500.00"), created.getCharges());
        assertEquals(new BigDecimal("100.00"), created.getTaxes());
        assertEquals(new BigDecimal("50.00"), created.getDiscount());
        assertEquals("PENDING", created.getPaymentStatus());
        assertEquals(2, created.getItems().size());
    }

    @Test
    void testCreateInvoice_DuplicateInvoiceNumber_ThrowsConflict() {
        Map<String, Object> req1 = Map.of(
                "invoiceNumber", "INV-TEST-9001",
                "customerName", "Pacific Express",
                "charges", 1000.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );
        invoiceService.createInvoice(req1, adminUser);

        Map<String, Object> req2 = Map.of(
                "invoiceNumber", "INV-TEST-9001",
                "customerName", "Atlantic Maritime",
                "charges", 2000.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );

        assertThrows(ResponseStatusException.class, () -> invoiceService.createInvoice(req2, adminUser));
    }

    @Test
    void testRecordPayment_PartialPayment_UpdatesStatusToPartiallyPaid() {
        Map<String, Object> invReq = Map.of(
                "invoiceNumber", "INV-TEST-PARTIAL",
                "customerName", "Apex Logistics",
                "charges", 2000.00,
                "taxes", 200.00,
                "discount", 0.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );
        Invoice inv = invoiceService.createInvoice(invReq, adminUser);

        Map<String, Object> payReq = Map.of(
                "invoiceId", inv.getId(),
                "amount", 1000.00,
                "reference", "PAY-PARTIAL-001",
                "paymentMethod", "WIRE_TRANSFER"
        );
        Payment payment = paymentService.recordPayment(payReq, adminUser);

        assertNotNull(payment.getId());

        Invoice updatedInv = invoiceService.getInvoiceById(inv.getId());
        assertEquals("PARTIALLY_PAID", updatedInv.getPaymentStatus());
        assertEquals(new BigDecimal("1200.00"), invoiceService.getInvoiceBalance(inv.getId()));
    }

    @Test
    void testRecordPayment_FullPayment_UpdatesStatusToPaid() {
        Map<String, Object> invReq = Map.of(
                "invoiceNumber", "INV-TEST-FULL",
                "customerName", "Oceanic Transport",
                "charges", 1500.00,
                "taxes", 150.00,
                "discount", 50.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );
        Invoice inv = invoiceService.createInvoice(invReq, adminUser);
        // Total = 1500 + 150 - 50 = 1600.00

        Map<String, Object> payReq = Map.of(
                "invoiceId", inv.getId(),
                "amount", 1600.00,
                "reference", "PAY-FULL-001",
                "paymentMethod", "ACH"
        );
        paymentService.recordPayment(payReq, adminUser);

        Invoice updatedInv = invoiceService.getInvoiceById(inv.getId());
        assertEquals("PAID", updatedInv.getPaymentStatus());
        assertEquals(new BigDecimal("0.00"), invoiceService.getInvoiceBalance(inv.getId()));
    }

    @Test
    void testRecordPayment_Overpayment_ThrowsBadRequest() {
        Map<String, Object> invReq = Map.of(
                "invoiceNumber", "INV-TEST-OVER",
                "customerName", "Standard Shipping Co",
                "charges", 500.00,
                "taxes", 50.00,
                "discount", 0.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );
        Invoice inv = invoiceService.createInvoice(invReq, adminUser);
        // Total = 550.00

        Map<String, Object> payReq = Map.of(
                "invoiceId", inv.getId(),
                "amount", 600.00,
                "reference", "PAY-OVER-001",
                "paymentMethod", "CREDIT_CARD"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> paymentService.recordPayment(payReq, adminUser));
        assertTrue(ex.getReason().contains("exceeds outstanding balance"));
    }

    @Test
    void testRecordPayment_DuplicateReference_ThrowsConflict() {
        Map<String, Object> invReq = Map.of(
                "invoiceNumber", "INV-DUP-REF",
                "customerName", "Naval Shipping",
                "charges", 1000.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        );
        Invoice inv = invoiceService.createInvoice(invReq, adminUser);

        Map<String, Object> payReq1 = Map.of(
                "invoiceId", inv.getId(),
                "amount", 200.00,
                "reference", "DUP-REF-100",
                "paymentMethod", "WIRE_TRANSFER"
        );
        paymentService.recordPayment(payReq1, adminUser);

        Map<String, Object> payReq2 = Map.of(
                "invoiceId", inv.getId(),
                "amount", 200.00,
                "reference", "DUP-REF-100",
                "paymentMethod", "CHECK"
        );

        assertThrows(ResponseStatusException.class, () -> paymentService.recordPayment(payReq2, adminUser));
    }

    @Test
    void testFinancialSummary_ReturnsAggregatedMetrics() {
        Invoice inv1 = invoiceService.createInvoice(Map.of(
                "invoiceNumber", "INV-METRIC-1",
                "customerName", "Client A",
                "charges", 1000.00,
                "taxes", 100.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        ), adminUser); // Total = 1100.00

        Invoice inv2 = invoiceService.createInvoice(Map.of(
                "invoiceNumber", "INV-METRIC-2",
                "customerName", "Client B",
                "charges", 2000.00,
                "taxes", 200.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        ), adminUser); // Total = 2200.00

        paymentService.recordPayment(Map.of(
                "invoiceId", inv1.getId(),
                "amount", 1100.00,
                "reference", "REF-SUMMARY-1"
        ), adminUser); // Paid = 1100.00

        paymentService.recordPayment(Map.of(
                "invoiceId", inv2.getId(),
                "amount", 500.00,
                "reference", "REF-SUMMARY-2"
        ), adminUser); // Paid = 500.00

        Map<String, Object> summary = financialDashboardService.getFinancialSummary();

        assertEquals(new BigDecimal("3300.00"), summary.get("totalInvoiced"));
        assertEquals(new BigDecimal("1600.00"), summary.get("totalCollected"));
        assertEquals(new BigDecimal("1700.00"), summary.get("outstandingBalance"));
        assertEquals(2, summary.get("totalInvoiceCount"));
        assertEquals(1L, summary.get("paidCount"));
        assertEquals(1L, summary.get("partiallyPaidCount"));
    }

    @Test
    @WithMockUser(username = "admin@portshipping.com", roles = {"ADMIN"})
    void testDownloadInvoicePdf_ReturnsPdfByteStream() throws Exception {
        Invoice inv = invoiceService.createInvoice(Map.of(
                "invoiceNumber", "INV-PDF-001",
                "customerName", "PDF Test Logistics",
                "charges", 3000.00,
                "taxes", 300.00,
                "dueDate", LocalDate.now().plusDays(30).toString()
        ), adminUser);

        mockMvc.perform(get("/api/invoices/" + inv.getId() + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"Invoice_INV-PDF-001.pdf\""));
    }

    @Test
    @WithMockUser(username = "admin@portshipping.com", roles = {"ADMIN"})
    void testCreateInvoiceRestApi_Returns201Created() throws Exception {
        Map<String, Object> body = Map.of(
                "customerName", "REST Customer Corp",
                "charges", 4500.00,
                "taxes", 450.00,
                "currency", "USD",
                "dueDate", LocalDate.now().plusDays(20).toString()
        );

        mockMvc.perform(post("/api/invoices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("REST Customer Corp"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "agent@portshipping.com", roles = {"SHIPPING_AGENT"})
    void testCreateInvoiceRestApi_ForbiddenForUnauthorizedRole() throws Exception {
        Map<String, Object> body = Map.of(
                "customerName", "Forbidden Corp",
                "charges", 1000.00,
                "dueDate", LocalDate.now().plusDays(20).toString()
        );

        mockMvc.perform(post("/api/invoices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
