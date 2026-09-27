package com.portshipping.dto;

import java.math.BigDecimal;
import java.util.Map;

public class FinancialAnalyticsDTO {
    private BigDecimal totalInvoiced;
    private BigDecimal totalCollected;
    private BigDecimal outstandingBalance;
    private BigDecimal overdueAmount;
    private long totalInvoiceCount;
    private long overdueCount;
    private Map<String, Long> statusCounts;

    public FinancialAnalyticsDTO() {}

    public FinancialAnalyticsDTO(BigDecimal totalInvoiced, BigDecimal totalCollected, BigDecimal outstandingBalance,
                                 BigDecimal overdueAmount, long totalInvoiceCount, long overdueCount,
                                 Map<String, Long> statusCounts) {
        this.totalInvoiced = totalInvoiced;
        this.totalCollected = totalCollected;
        this.outstandingBalance = outstandingBalance;
        this.overdueAmount = overdueAmount;
        this.totalInvoiceCount = totalInvoiceCount;
        this.overdueCount = overdueCount;
        this.statusCounts = statusCounts;
    }

    public BigDecimal getTotalInvoiced() { return totalInvoiced; }
    public void setTotalInvoiced(BigDecimal totalInvoiced) { this.totalInvoiced = totalInvoiced; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }

    public long getTotalInvoiceCount() { return totalInvoiceCount; }
    public void setTotalInvoiceCount(long totalInvoiceCount) { this.totalInvoiceCount = totalInvoiceCount; }

    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }

    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
}
