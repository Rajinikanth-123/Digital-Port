package com.portshipping.service;

import com.portshipping.entity.Invoice;
import com.portshipping.repository.InvoiceRepository;
import com.portshipping.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FinancialDashboardService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public FinancialDashboardService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> getFinancialSummary() {
        BigDecimal totalInvoiced = invoiceRepository.calculateTotalInvoiced();
        if (totalInvoiced == null) totalInvoiced = BigDecimal.ZERO;
        totalInvoiced = totalInvoiced.setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalCollected = paymentRepository.sumTotalPayments();
        if (totalCollected == null) totalCollected = BigDecimal.ZERO;
        totalCollected = totalCollected.setScale(2, RoundingMode.HALF_UP);

        BigDecimal outstandingBalance = totalInvoiced.subtract(totalCollected);
        if (outstandingBalance.compareTo(BigDecimal.ZERO) < 0) outstandingBalance = BigDecimal.ZERO;

        LocalDate today = LocalDate.now();
        long overdueCount = invoiceRepository.countOverdueInvoices(today);
        BigDecimal overdueAmount = invoiceRepository.sumOverdueAmount(today);
        if (overdueAmount == null) overdueAmount = BigDecimal.ZERO;

        List<Invoice> allInvoices = invoiceRepository.findAll();
        long paidCount = allInvoices.stream().filter(i -> "PAID".equalsIgnoreCase(i.getPaymentStatus())).count();
        long partiallyPaidCount = allInvoices.stream().filter(i -> "PARTIALLY_PAID".equalsIgnoreCase(i.getPaymentStatus())).count();
        long pendingCount = allInvoices.stream().filter(i -> "PENDING".equalsIgnoreCase(i.getPaymentStatus())).count();
        long cancelledCount = allInvoices.stream().filter(i -> "CANCELLED".equalsIgnoreCase(i.getPaymentStatus())).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("currency", "USD");
        summary.put("totalInvoiced", totalInvoiced);
        summary.put("totalCollected", totalCollected);
        summary.put("outstandingBalance", outstandingBalance);
        summary.put("overdueCount", overdueCount);
        summary.put("overdueAmount", overdueAmount.setScale(2, RoundingMode.HALF_UP));
        summary.put("totalInvoiceCount", allInvoices.size());
        summary.put("paidCount", paidCount);
        summary.put("partiallyPaidCount", partiallyPaidCount);
        summary.put("pendingCount", pendingCount);
        summary.put("cancelledCount", cancelledCount);

        return summary;
    }
}
