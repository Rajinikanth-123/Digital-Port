package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "berths", uniqueConstraints = @UniqueConstraint(name = "uq_berths_terminal_name", columnNames = {"terminal_id", "name"}))
public class Berth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "terminal_id", nullable = false)
    private Terminal terminal;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "max_length_m", nullable = false)
    private Double maxLengthM;

    @Column(nullable = false, length = 30)
    private String status = "AVAILABLE";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected Berth() {}

    public Berth(Terminal terminal, String name, Double maxLengthM) {
        this.terminal = terminal;
        this.name = name;
        this.maxLengthM = maxLengthM;
    }

    public Berth(Terminal terminal, String name, Double maxLengthM, String status) {
        this.terminal = terminal;
        this.name = name;
        this.maxLengthM = maxLengthM;
        this.status = status != null && !status.isBlank() ? status : "AVAILABLE";
    }

    public Long getId() { return id; }

    public Terminal getTerminal() { return terminal; }
    public void setTerminal(Terminal terminal) { this.terminal = terminal; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getMaxLengthM() { return maxLengthM; }
    public void setMaxLengthM(Double maxLengthM) { this.maxLengthM = maxLengthM; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
