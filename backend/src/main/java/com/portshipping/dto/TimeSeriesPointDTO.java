package com.portshipping.dto;

import java.math.BigDecimal;

public class TimeSeriesPointDTO {
    private String label;
    private BigDecimal amount;
    private long count;

    public TimeSeriesPointDTO() {}

    public TimeSeriesPointDTO(String label, BigDecimal amount, long count) {
        this.label = label;
        this.amount = amount;
        this.count = count;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
