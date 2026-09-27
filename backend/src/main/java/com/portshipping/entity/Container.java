package com.portshipping.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "containers", uniqueConstraints = {
    @UniqueConstraint(name = "uq_containers_number", columnNames = "container_number")
})
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "container_number", nullable = false, length = 20, unique = true)
    private String containerNumber;

    @Column(length = 10, nullable = false)
    private String size = "40FT";

    @Column(length = 30, nullable = false)
    private String type = "DRY";

    @Column(name = "size_code", length = 10)
    private String sizeCode;

    @Column(name = "type_code", length = 30)
    private String typeCode;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightKg = BigDecimal.ZERO;

    @Column(length = 120)
    private String location;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(length = 30, nullable = false)
    private String status = "AVAILABLE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected Container() {}

    public Container(String containerNumber, String size, String type, String status, Shipment shipment) {
        this.containerNumber = containerNumber;
        this.size = size;
        this.type = type;
        this.status = status != null ? status : "AVAILABLE";
        this.shipment = shipment;
        this.weightKg = BigDecimal.ZERO;
    }

    public Container(String containerNumber, String size, String type, BigDecimal weightKg, String location, String status, Shipment shipment) {
        this.containerNumber = containerNumber;
        this.size = size;
        this.type = type;
        this.weightKg = weightKg != null ? weightKg : BigDecimal.ZERO;
        this.location = location;
        this.status = status != null ? status : "AVAILABLE";
        this.shipment = shipment;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerNumber() { return containerNumber; }
    public void setContainerNumber(String containerNumber) { this.containerNumber = containerNumber; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSizeCode() { return sizeCode; }
    public void setSizeCode(String sizeCode) { this.sizeCode = sizeCode; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
