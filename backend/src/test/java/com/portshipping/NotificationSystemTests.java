package com.portshipping;

import com.portshipping.entity.*;
import com.portshipping.repository.*;
import com.portshipping.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotificationSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private CustomsClearanceService customsClearanceService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OverdueInvoiceScheduler overdueInvoiceScheduler;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();

        user1 = userRepository.findByEmailIgnoreCase("user1@portshipping.com").orElseGet(() -> {
            User u = new User("User One", "user1@portshipping.com", "pass");
            u.setRole("ADMIN");
            return userRepository.save(u);
        });

        user2 = userRepository.findByEmailIgnoreCase("user2@portshipping.com").orElseGet(() -> {
            User u = new User("User Two", "user2@portshipping.com", "pass");
            u.setRole("SHIPPING_AGENT");
            return userRepository.save(u);
        });
    }

    @Test
    void testSendNotification_CreatesAndRetrieves() {
        Notification n = notificationService.sendNotification(user1, "Welcome Alert", "Your account setup is complete.", "SYSTEM", "User", user1.getId());
        assertNotNull(n);
        assertNotNull(n.getId());
        assertFalse(n.getReadStatus());
        assertFalse(n.getArchived());

        long unread = notificationService.getUnreadCount(user1);
        assertEquals(1, unread);

        Page<Notification> page = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("Welcome Alert", page.getContent().get(0).getTitle());
    }

    @Test
    void testMarkAsRead_UpdatesReadStatusAndTimestamp() {
        Notification n = notificationService.sendNotification(user1, "Shipment Arrived", "Shipment #SH-100 arrived at berth.", "SHIPMENT", "Shipment", 100L);
        assertFalse(n.getReadStatus());

        Notification read = notificationService.markAsRead(n.getId(), user1);
        assertTrue(read.getReadStatus());
        assertNotNull(read.getReadAt());
        assertEquals(0, notificationService.getUnreadCount(user1));
    }

    @Test
    void testMarkAllAsRead_MarksAllUnreadNotifications() {
        notificationService.sendNotification(user1, "Alert 1", "Message 1", "SYSTEM", "System", 1L);
        notificationService.sendNotification(user1, "Alert 2", "Message 2", "SYSTEM", "System", 2L);
        assertEquals(2, notificationService.getUnreadCount(user1));

        notificationService.markAllAsRead(user1);
        assertEquals(0, notificationService.getUnreadCount(user1));
    }

    @Test
    void testArchiveAndDeleteNotification() {
        Notification n = notificationService.sendNotification(user1, "Archivable Alert", "Message to archive", "SYSTEM", "System", 1L);

        Notification archived = notificationService.archiveNotification(n.getId(), user1);
        assertTrue(archived.getArchived());

        notificationService.deleteNotification(n.getId(), user1);
        assertFalse(notificationRepository.existsById(n.getId()));
    }

    @Test
    void testCrossUserAccess_ThrowsForbidden() {
        Notification n = notificationService.sendNotification(user1, "User1 Secret Alert", "Private info", "SYSTEM", "System", 1L);

        // User2 trying to mark User1's notification as read
        assertThrows(ResponseStatusException.class, () -> notificationService.markAsRead(n.getId(), user2));

        // User2 trying to archive User1's notification
        assertThrows(ResponseStatusException.class, () -> notificationService.archiveNotification(n.getId(), user2));

        // User2 trying to delete User1's notification
        assertThrows(ResponseStatusException.class, () -> notificationService.deleteNotification(n.getId(), user2));
    }

    @Test
    void testDeduplication_PreventsIdenticalAlertsWithinTimeWindow() {
        Notification n1 = notificationService.sendNotification(user1, "Duplicate Test", "Alert details", "VESSEL", "Vessel", 50L);
        assertNotNull(n1);

        // Immediate duplicate attempt should return null
        Notification n2 = notificationService.sendNotification(user1, "Duplicate Test", "Alert details", "VESSEL", "Vessel", 50L);
        assertNull(n2);

        // Total notifications for user1 should remain 1
        assertEquals(1, notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void testShipmentEventIntegration_GeneratesNotification() {
        Shipment s = shipmentService.createShipment("TRK-NOTIF-1", "REF-NOTIF-1", "Origin Port", "Destination Port", "Shipper", "Consignee", LocalDate.now().plusDays(5), "REGISTERED", 2, null);
        assertNotNull(s);

        // Check if user1 received notification
        Page<Notification> notifs = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertTrue(notifs.getContent().stream().anyMatch(n -> n.getTitle().contains("New Shipment Created")));

        // Update shipment status
        shipmentService.updateShipment(s.getId(), null, null, null, null, null, null, "IN_TRANSIT", null, null);
        Page<Notification> updatedNotifs = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertTrue(updatedNotifs.getContent().stream().anyMatch(n -> n.getTitle().contains("Shipment Status Updated")));
    }

    @Test
    void testInvoiceAndPaymentEventIntegration_GeneratesNotifications() {
        Invoice inv = invoiceService.createInvoice(Map.of(
                "invoiceNumber", "INV-NOTIF-01",
                "customerName", "Apex Shipping",
                "charges", 1000.00,
                "taxes", 100.00,
                "discount", 0.00,
                "dueDate", LocalDate.now().plusDays(10).toString()
        ), user1);

        assertNotNull(inv);
        Page<Notification> invNotifs = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertTrue(invNotifs.getContent().stream().anyMatch(n -> n.getTitle().contains("Invoice Issued")));

        paymentService.recordPayment(Map.of(
                "invoiceId", inv.getId(),
                "amount", 1100.00,
                "reference", "PAY-NOTIF-01",
                "paymentMethod", "CREDIT_CARD"
        ), user1);

        Page<Notification> payNotifs = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertTrue(payNotifs.getContent().stream().anyMatch(n -> n.getTitle().contains("Payment Received")));
    }

    @Test
    void testOverdueInvoiceScheduler_DetectsOverdueAndSendsNotification() {
        Invoice overdueInv = new Invoice("INV-OVERDUE-01", "Late Customer", null, new BigDecimal("2000.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now().minusDays(5), "USD", "PENDING");
        invoiceRepository.save(overdueInv);

        overdueInvoiceScheduler.checkAndAlertOverdueInvoices();

        Invoice updated = invoiceRepository.findById(overdueInv.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("OVERDUE", updated.getPaymentStatus());

        Page<Notification> notifs = notificationService.getUserNotifications(user1, false, null, false, PageRequest.of(0, 10));
        assertTrue(notifs.getContent().stream().anyMatch(n -> n.getTitle().contains("Overdue Invoice Alert")));
    }

    @Test
    @WithMockUser(username = "user1@portshipping.com", roles = {"ADMIN"})
    void testNotificationRestEndpoints_Success() throws Exception {
        Notification n = notificationService.sendNotification(user1, "Endpoint Test", "Rest test message", "SYSTEM", "System", 99L);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Endpoint Test"));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(patch("/api/notifications/" + n.getId() + "/read").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readStatus").value(true));

        mockMvc.perform(patch("/api/notifications/" + n.getId() + "/archive").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(delete("/api/notifications/" + n.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
