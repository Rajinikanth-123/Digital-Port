package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "terminals", uniqueConstraints = @UniqueConstraint(name = "uq_terminals_code", columnNames = "code"))
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "port_id", nullable = false)
    private Port port;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(name = "terminal_type", nullable = false, length = 50)
    private String terminalType = "CONTAINER";

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected Terminal() {}

    public Terminal(Port port, String name, String code) {
        this.port = port;
        this.name = name;
        this.code = code;
    }

    public Terminal(Port port, String name, String code, String terminalType, Integer capacity) {
        this.port = port;
        this.name = name;
        this.code = code;
        this.terminalType = terminalType != null && !terminalType.isBlank() ? terminalType : "CONTAINER";
        this.capacity = capacity != null ? capacity : 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public Port getPort() { return port; }
    public void setPort(Port port) { this.port = port; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTerminalType() { return terminalType; }
    public void setTerminalType(String terminalType) { this.terminalType = terminalType; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
