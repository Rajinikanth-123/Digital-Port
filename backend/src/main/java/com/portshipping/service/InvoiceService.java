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
import java.time.Year;
import java.util.*;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          PaymentRepository paymentRepository,
                          ShipmentRepository shipmentRepository,
                          AuditLogRepository auditLogRepository,
                          NotificationService notificationService,
                          ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoices(String search, String status, Pageable pageable) {
        if ((search != null && !search.isBlank()) || (status != null && !status.isBlank())) {
            return invoiceRepository.searchInvoices(search, status, pageable);
        }
        return invoiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with ID " + id));
    }

    public Invoice createInvoice(Map<String, Object> req, User currentUser) {
        String customerName = (String) req.get("customerName");
        if (customerName == null || customerName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer name is required.");
        }

        String invoiceNumber = (String) req.get("invoiceNumber");
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            invoiceNumber = generateUniqueInvoiceNumber();
        } else {
            invoiceNumber = invoiceNumber.trim().toUpperCase();
            if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice number already exists: " + invoiceNumber);
            }
        }

        Shipment shipment = null;
        if (req.containsKey("shipmentId") && req.get("shipmentId") != null) {
            Long shipmentId = Long.valueOf(req.get("shipmentId").toString());
            shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipment not found with ID " + shipmentId));
        } else if (req.containsKey("shipment") && req.get("shipment") instanceof Map) {
            Map<?, ?> sMap = (Map<?, ?>) req.get("shipment");
            if (sMap.get("id") != null) {
                Long sId = Long.valueOf(sMap.get("id").toString());
                shipment = shipmentRepository.findById(sId).orElse(null);
            }
        }

        LocalDate dueDate;
        if (req.get("dueDate") != null) {
            dueDate = LocalDate.parse(req.get("dueDate").toString());
        } else {
            dueDate = LocalDate.now().plusDays(30);
        }

        String currency = req.get("currency") != null ? req.get("currency").toString() : "USD";
        String status = req.get("paymentStatus") != null ? req.get("paymentStatus").toString().toUpperCase() : "PENDING";

        BigDecimal charges = parseBigDecimal(req.get("charges"));
        BigDecimal taxes = parseBigDecimal(req.get("taxes"));
        BigDecimal discount = parseBigDecimal(req.get("discount"));

        Invoice invoice = new Invoice(invoiceNumber, customerName.trim(), shipment, charges, taxes, discount, dueDate, currency, status);
        if (req.get("notes") != null) {
            invoice.setNotes(req.get("notes").toString());
        }

        // Process line items if submitted
        if (req.containsKey("items") && req.get("items") instanceof List) {
            List<?> rawItems = (List<?>) req.get("items");
            BigDecimal calculatedCharges = BigDecimal.ZERO;

            for (Object rawItem : rawItems) {
                if (rawItem instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) rawItem;
                    String desc = itemMap.get("description") != null ? itemMap.get("description").toString() : "Service Line Item";
                    BigDecimal qty = parseBigDecimal(itemMap.get("quantity"));
                    if (qty.compareTo(BigDecimal.ZERO) <= 0) qty = BigDecimal.ONE;

                    BigDecimal unitPrice = parseBigDecimal(itemMap.get("unitPrice"));
                    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price cannot be negative.");
                    }

                    BigDecimal lineTotal = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    calculatedCharges = calculatedCharges.add(lineTotal);

                    InvoiceItem item = new InvoiceItem(invoice, desc, qty, unitPrice);
                    invoice.addItem(item);
                }
            }

            if (!rawItems.isEmpty()) {
                invoice.setCharges(calculatedCharges);
            }
        }

        Invoice saved = invoiceRepository.save(invoice);
        logAudit(currentUser, "CREATE_INVOICE", "Invoice", saved.getId(), "Issued invoice #" + saved.getInvoiceNumber() + " for " + saved.getCustomerName());

        notificationService.notifyUsersByRole(
                List.of("ADMIN", "PORT_MANAGER", "FINANCE_OFFICER"),
                "Invoice Issued: #" + saved.getInvoiceNumber(),
                "New invoice #" + saved.getInvoiceNumber() + " issued for " + saved.getCustomerName() + ".",
                "INVOICE", "Invoice", saved.getId(), "NORMAL"
        );

        return saved;
    }

    public Invoice updateInvoice(Long id, Map<String, Object> req, User currentUser) {
        Invoice invoice = getInvoiceById(id);

        if ("PAID".equalsIgnoreCase(invoice.getPaymentStatus()) || "CANCELLED".equalsIgnoreCase(invoice.getPaymentStatus())) {
            if (currentUser != null && !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit invoice with status " + invoice.getPaymentStatus());
            }
        }

        if (req.containsKey("customerName") && req.get("customerName") != null) {
            invoice.setCustomerName(req.get("customerName").toString().trim());
        }

        if (req.containsKey("dueDate") && req.get("dueDate") != null) {
            invoice.setDueDate(LocalDate.parse(req.get("dueDate").toString()));
        }

        if (req.containsKey("currency") && req.get("currency") != null) {
            invoice.setCurrency(req.get("currency").toString());
        }

        if (req.containsKey("paymentStatus") && req.get("paymentStatus") != null) {
            invoice.setPaymentStatus(req.get("paymentStatus").toString().toUpperCase());
        }

        if (req.containsKey("notes")) {
            invoice.setNotes(req.get("notes") != null ? req.get("notes").toString() : null);
        }

        if (req.containsKey("charges")) invoice.setCharges(parseBigDecimal(req.get("charges")));
        if (req.containsKey("taxes")) invoice.setTaxes(parseBigDecimal(req.get("taxes")));
        if (req.containsKey("discount")) invoice.setDiscount(parseBigDecimal(req.get("discount")));

        // Update line items if provided
        if (req.containsKey("items") && req.get("items") instanceof List) {
            invoice.getItems().clear();
            List<?> rawItems = (List<?>) req.get("items");
            BigDecimal calculatedCharges = BigDecimal.ZERO;

            for (Object rawItem : rawItems) {
                if (rawItem instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) rawItem;
                    String desc = itemMap.get("description") != null ? itemMap.get("description").toString() : "Line Item";
                    BigDecimal qty = parseBigDecimal(itemMap.get("quantity"));
                    if (qty.compareTo(BigDecimal.ZERO) <= 0) qty = BigDecimal.ONE;

                    BigDecimal unitPrice = parseBigDecimal(itemMap.get("unitPrice"));
                    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price cannot be negative.");
                    }

                    BigDecimal lineTotal = qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
                    calculatedCharges = calculatedCharges.add(lineTotal);

                    InvoiceItem item = new InvoiceItem(invoice, desc, qty, unitPrice);
                    invoice.addItem(item);
                }
            }
            if (!rawItems.isEmpty()) {
                invoice.setCharges(calculatedCharges);
            }
        }

        Invoice updated = invoiceRepository.save(invoice);
        logAudit(currentUser, "UPDATE_INVOICE", "Invoice", updated.getId(), "Updated invoice #" + updated.getInvoiceNumber());
        return updated;
    }

    public Invoice cancelInvoice(Long id, User currentUser) {
        Invoice invoice = getInvoiceById(id);

        BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceId(id);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel invoice with recorded payments.");
        }

        invoice.setPaymentStatus("CANCELLED");
        Invoice saved = invoiceRepository.save(invoice);
        logAudit(currentUser, "CANCEL_INVOICE", "Invoice", saved.getId(), "Cancelled invoice #" + saved.getInvoiceNumber());
        return saved;
    }

    public void deleteInvoice(Long id, User currentUser) {
        Invoice invoice = getInvoiceById(id);

        BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceId(id);
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete invoice with recorded payments. Please cancel it instead.");
        }

        if ("PAID".equalsIgnoreCase(invoice.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a settled invoice.");
        }

        invoiceRepository.delete(invoice);
        logAudit(currentUser, "DELETE_INVOICE", "Invoice", id, "Deleted invoice #" + invoice.getInvoiceNumber());
    }

    public BigDecimal getInvoiceBalance(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        BigDecimal total = calculateStoredTotal(invoice);
        BigDecimal paid = paymentRepository.sumAmountByInvoiceId(invoiceId);
        BigDecimal balance = total.subtract(paid);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    public BigDecimal calculateStoredTotal(Invoice invoice) {
        BigDecimal charges = invoice.getCharges() != null ? invoice.getCharges() : BigDecimal.ZERO;
        BigDecimal taxes = invoice.getTaxes() != null ? invoice.getTaxes() : BigDecimal.ZERO;
        BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
        return charges.add(taxes).subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateUniqueInvoiceNumber() {
        int year = Year.now().getValue();
        long count = invoiceRepository.count() + 1;
        String number;
        do {
            number = String.format("INV-%d-%04d", year, count++);
        } while (invoiceRepository.existsByInvoiceNumber(number));
        return number;
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
