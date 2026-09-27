package com.portshipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          InvoiceService invoiceService,
                          NotificationService notificationService,
                          AuditLogRepository auditLogRepository,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return paymentRepository.searchPayments(search, pageable);
        }
        return paymentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment receipt not found with ID " + id));
    }

    public Payment recordPayment(Map<String, Object> req, User currentUser) {
        Long invoiceId = null;
        if (req.containsKey("invoiceId") && req.get("invoiceId") != null) {
            invoiceId = Long.valueOf(req.get("invoiceId").toString());
        } else if (req.containsKey("invoice") && req.get("invoice") instanceof Map) {
            Map<?, ?> invMap = (Map<?, ?>) req.get("invoice");
            if (invMap.get("id") != null) {
                invoiceId = Long.valueOf(invMap.get("id").toString());
            }
        }

        if (invoiceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target invoice ID is required.");
        }

        final Long targetInvoiceId = invoiceId;
        Invoice invoice = invoiceRepository.findById(targetInvoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target invoice not found with ID " + targetInvoiceId));

        if ("CANCELLED".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot record payment against a cancelled invoice.");
        }

        BigDecimal amount = parseBigDecimal(req.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero.");
        }

        String reference = (String) req.get("reference");
        if (reference == null || reference.isBlank()) {
            reference = "PAY-" + System.currentTimeMillis();
        } else {
            reference = reference.trim().toUpperCase();
            if (paymentRepository.existsByReference(reference)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment reference code already exists: " + reference);
            }
        }

        // Overpayment check
        BigDecimal invoiceTotal = invoiceService.calculateStoredTotal(invoice);
        BigDecimal currentPaid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        BigDecimal remainingBalance = invoiceTotal.subtract(currentPaid);

        if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) remainingBalance = BigDecimal.ZERO;

        if (amount.compareTo(remainingBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Payment amount ($%.2f) exceeds outstanding balance ($%.2f). Overpayment is not allowed.", amount, remainingBalance));
        }

        String paymentMethod = req.get("paymentMethod") != null ? req.get("paymentMethod").toString() : "WIRE_TRANSFER";
        LocalDateTime paidAt = req.get("paidAt") != null ? LocalDateTime.parse(req.get("paidAt").toString()) : LocalDateTime.now();

        Payment payment = new Payment(invoice, amount, paidAt, paymentMethod, reference);
        Payment saved = paymentRepository.save(payment);

        // Update Invoice status dynamically
        recalculateAndSaveInvoiceStatus(invoice);

        logAudit(currentUser, "RECORD_PAYMENT", "Payment", saved.getId(),
                "Recorded payment of $" + amount + " for invoice #" + invoice.getInvoiceNumber() + " (Ref: " + reference + ")");

        notificationService.notifyUsersByRole(
                List.of("ADMIN", "PORT_MANAGER", "FINANCE_OFFICER"),
                "Payment Received: " + reference,
                "Payment of $" + amount + " received for Invoice #" + invoice.getInvoiceNumber() + " (Status: " + invoice.getPaymentStatus() + ").",
                "PAYMENT", "Payment", saved.getId(), "NORMAL"
        );

        return saved;
    }

    public Payment updatePayment(Long id, Map<String, Object> req, User currentUser) {
        Payment payment = getPaymentById(id);
        Invoice invoice = payment.getInvoice();

        if (req.containsKey("reference") && req.get("reference") != null) {
            String newRef = req.get("reference").toString().trim().toUpperCase();
            if (!newRef.equalsIgnoreCase(payment.getReference()) && paymentRepository.existsByReference(newRef)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment reference already exists: " + newRef);
            }
            payment.setReference(newRef);
        }

        if (req.containsKey("paymentMethod") && req.get("paymentMethod") != null) {
            payment.setPaymentMethod(req.get("paymentMethod").toString());
        }

        if (req.containsKey("paidAt") && req.get("paidAt") != null) {
            payment.setPaidAt(LocalDateTime.parse(req.get("paidAt").toString()));
        }

        if (req.containsKey("amount") && req.get("amount") != null) {
            BigDecimal newAmount = parseBigDecimal(req.get("amount"));
            if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero.");
            }

            BigDecimal invoiceTotal = invoiceService.calculateStoredTotal(invoice);
            BigDecimal otherPaidSum = paymentRepository.sumAmountByInvoiceId(invoice.getId()).subtract(payment.getAmount());
            BigDecimal newBalance = invoiceTotal.subtract(otherPaidSum);

            if (newAmount.compareTo(newBalance) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Updated amount ($%.2f) exceeds remaining invoice balance ($%.2f).", newAmount, newBalance));
            }
            payment.setAmount(newAmount);
        }

        Payment updated = paymentRepository.save(payment);
        recalculateAndSaveInvoiceStatus(invoice);

        logAudit(currentUser, "UPDATE_PAYMENT", "Payment", updated.getId(), "Updated payment receipt #" + updated.getReference());
        return updated;
    }

    public void deletePayment(Long id, User currentUser) {
        Payment payment = getPaymentById(id);
        Invoice invoice = payment.getInvoice();

        paymentRepository.delete(payment);
        recalculateAndSaveInvoiceStatus(invoice);

        logAudit(currentUser, "DELETE_PAYMENT", "Payment", id, "Deleted payment receipt #" + payment.getReference());
    }

    public void recalculateAndSaveInvoiceStatus(Invoice invoice) {
        BigDecimal invoiceTotal = invoiceService.calculateStoredTotal(invoice);
        BigDecimal totalPaid = paymentRepository.sumAmountByInvoiceId(invoice.getId());

        if (totalPaid.compareTo(invoiceTotal) >= 0 && invoiceTotal.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("PAID");
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setPaymentStatus("PARTIALLY_PAID");
        } else {
            if (invoice.getDueDate() != null && invoice.getDueDate().isBefore(LocalDate.now())) {
                invoice.setPaymentStatus("OVERDUE");
            } else {
                invoice.setPaymentStatus("PENDING");
            }
        }
        invoiceRepository.save(invoice);
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void logAudit(User user, String action, String entityName, Long entityId, String detailsStr) {
        try {
            Map<String, Object> map = Map.of("details", detailsStr != null ? detailsStr : "");
            AuditLog log = new AuditLog(user, action, entityName, entityId, objectMapper.writeValueAsString(map));
            auditLogRepository.save(log);
        } catch (Exception ignored) {}
    }
}
