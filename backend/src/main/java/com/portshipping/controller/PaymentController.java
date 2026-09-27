package com.portshipping.controller;

import com.portshipping.entity.Payment;
import com.portshipping.entity.User;
import com.portshipping.service.PaymentService;
import com.portshipping.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<Payment>> getPayments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<Payment> payments = paymentService.getPayments(search, PageRequest.of(page, size, Sort.by("id").descending()));
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<Payment> recordPayment(
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        Payment payment = paymentService.recordPayment(req, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PORT_MANAGER', 'FINANCE_OFFICER')")
    public ResponseEntity<Payment> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> req,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        Payment payment = paymentService.updatePayment(id, req, currentUser);
        return ResponseEntity.ok(payment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        User currentUser = resolveUser(principal);
        paymentService.deletePayment(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(UserDetails principal) {
        if (principal == null) return null;
        return userService.findByEmail(principal.getUsername()).orElse(null);
    }
}
