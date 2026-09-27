package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "shipments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_shipments_tracking", columnNames = "tracking_number"),
    @UniqueConstraint(name = "uq_shipments_reference", columnNames = "reference_number")
})
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, length = 40, unique = true)
    private String trackingNumber;

    @Column(name = "reference_number", length = 40, unique = true)
    private String referenceNumber;

    @Column(nullable = false, length = 120)
    private String origin;

    @Column(nullable = false, length = 120)
    private String destination;

    @Lob
    @Column(name = "cargo_details", columnDefinition = "TEXT")
    private String cargoDetails;

    @Column(length = 160)
    private String shipper;

    @Column(length = 160)
    private String consignee;

    @Column(name = "estimated_arrival")
    private LocalDate estimatedArrival;

    @Column(nullable = false, length = 30)
    private String status = "REGISTERED";

    @Column(name = "container_count", nullable = false)
    private Integer containerCount = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vessel_id")
    private Vessel vessel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected Shipment() {}

    public Shipment(String ref, String o, String d, String s, Integer c, Vessel v) {
        this.trackingNumber = ref;
        this.referenceNumber = ref;
        this.origin = o;
        this.destination = d;
        this.status = s != null ? s : "REGISTERED";
        this.containerCount = c != null ? c : 0;
        this.vessel = v;
    }

    public Shipment(String trackingNumber, String referenceNumber, String origin, String destination, String shipper, String consignee, LocalDate estimatedArrival, String status, Integer containerCount, Vessel vessel) {
        this.trackingNumber = trackingNumber;
        this.referenceNumber = referenceNumber;
        this.origin = origin;
        this.destination = destination;
        this.shipper = shipper;
        this.consignee = consignee;
        this.estimatedArrival = estimatedArrival;
        this.status = status != null ? status : "REGISTERED";
        this.containerCount = containerCount != null ? containerCount : 0;
        this.vessel = vessel;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getCargoDetails() { return cargoDetails; }
    public void setCargoDetails(String cargoDetails) { this.cargoDetails = cargoDetails; }

    public String getShipper() { return shipper; }
    public void setShipper(String shipper) { this.shipper = shipper; }

    public String getConsignee() { return consignee; }
    public void setConsignee(String consignee) { this.consignee = consignee; }

    public LocalDate getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(LocalDate estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getContainerCount() { return containerCount; }
    public void setContainerCount(Integer containerCount) { this.containerCount = containerCount; }

    public Vessel getVessel() { return vessel; }
    public void setVessel(Vessel vessel) { this.vessel = vessel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
