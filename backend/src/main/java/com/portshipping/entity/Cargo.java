package com.portshipping.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cargo")
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "cargo_type", length = 80)
    private String cargoType = "GENERAL";

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightKg = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Cargo() {}

    public Cargo(Shipment shipment, String description, BigDecimal weightKg) {
        this.shipment = shipment;
        this.description = description;
        this.weightKg = weightKg != null ? weightKg : BigDecimal.ZERO;
    }

    public Cargo(Shipment shipment, String description, String cargoType, Integer quantity, BigDecimal weightKg) {
        this.shipment = shipment;
        this.description = description;
        this.cargoType = cargoType != null ? cargoType : "GENERAL";
        this.quantity = quantity != null ? quantity : 1;
        this.weightKg = weightKg != null ? weightKg : BigDecimal.ZERO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCargoType() { return cargoType; }
    public void setCargoType(String cargoType) { this.cargoType = cargoType; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
