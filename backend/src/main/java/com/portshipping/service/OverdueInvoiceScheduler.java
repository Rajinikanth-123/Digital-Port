package com.portshipping.service;

import com.portshipping.entity.Invoice;
import com.portshipping.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueInvoiceScheduler {

    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;

    public OverdueInvoiceScheduler(InvoiceRepository invoiceRepository, NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedRate = 3600000) // Run hourly
    @Transactional
    public void checkAndAlertOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> invoices = invoiceRepository.findAll();

        for (Invoice invoice : invoices) {
            String status = invoice.getPaymentStatus() != null ? invoice.getPaymentStatus() : "PENDING";
            if (!"PAID".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status)) {
                if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(today)) {
                    if (!"OVERDUE".equalsIgnoreCase(status)) {
                        invoice.setPaymentStatus("OVERDUE");
                        invoiceRepository.save(invoice);
                    }

                    // Send alert to Finance and Management roles
                    String title = "Overdue Invoice Alert: #" + invoice.getInvoiceNumber();
                    String msg = String.format("Invoice #%s for customer '%s' was due on %s and remains unpaid.",
                            invoice.getInvoiceNumber(), invoice.getCustomerName(), invoice.getDueDate().toString());

                    notificationService.notifyUsersByRole(
                            List.of("ADMIN", "PORT_MANAGER", "FINANCE_OFFICER"),
                            title, msg, "INVOICE", "Invoice", invoice.getId(), "HIGH"
                    );
                }
            }
        }
    }
}
