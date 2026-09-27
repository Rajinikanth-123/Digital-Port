package com.portshipping.controller;

import com.portshipping.entity.Invoice;
import com.portshipping.entity.User;
import com.portshipping.service.InvoicePdfService;
import com.portshipping.service.InvoiceService;
import com.portshipping.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final UserService userService;

    public InvoiceController(InvoiceService invoiceService, InvoicePdfService invoicePdfService, UserService userService) {
        this.invoiceService = invoiceService;
        this.invoicePdfService = invoicePdfService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<Invoice>> getInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Invoice> invoices = invoiceService.getInvoices(search, status, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<Invoice> createInvoice(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        Invoice invoice = invoiceService.createInvoice(req, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<Invoice> updateInvoice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        Invoice invoice = invoiceService.updateInvoice(id, req, currentUser);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<Invoice> cancelInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        Invoice invoice = invoiceService.cancelInvoice(id, currentUser);
        return ResponseEntity.ok(invoice);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        invoiceService.deleteInvoice(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id);
            byte[] pdfContent = invoicePdfService.generateInvoicePdf(invoice);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Invoice_" + invoice.getInvoiceNumber() + ".pdf");
            headers.setContentLength(pdfContent.length);

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) return null;
        return userService.findByEmail(principal.getUsername()).orElse(null);
    }
}
