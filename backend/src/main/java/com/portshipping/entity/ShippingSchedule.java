package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_schedules")
public class ShippingSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vessel_id", nullable = false)
    private Vessel vessel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "berth_id")
    private Berth berth;

    @Column(name = "arrival_estimated", nullable = false)
    private LocalDateTime arrivalEstimated;

    @Column(name = "departure_estimated")
    private LocalDateTime departureEstimated;

    @Column(name = "arrival_actual")
    private LocalDateTime arrivalActual;

    @Column(name = "departure_actual")
    private LocalDateTime departureActual;

    @Column(nullable = false, length = 30)
    private String status = "SCHEDULED"; // SCHEDULED, BERTHED, DEPARTED, DELAYED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ShippingSchedule() {}

    public ShippingSchedule(Vessel vessel, Berth berth, LocalDateTime arrivalEstimated, LocalDateTime departureEstimated, String status) {
        this.vessel = vessel;
        this.berth = berth;
        this.arrivalEstimated = arrivalEstimated;
        this.departureEstimated = departureEstimated;
        this.status = status != null && !status.isBlank() ? status : "SCHEDULED";
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vessel getVessel() { return vessel; }
    public void setVessel(Vessel vessel) { this.vessel = vessel; }

    public Berth getBerth() { return berth; }
    public void setBerth(Berth berth) { this.berth = berth; }

    public LocalDateTime getArrivalEstimated() { return arrivalEstimated; }
    public void setArrivalEstimated(LocalDateTime arrivalEstimated) { this.arrivalEstimated = arrivalEstimated; }

    public LocalDateTime getDepartureEstimated() { return departureEstimated; }
    public void setDepartureEstimated(LocalDateTime departureEstimated) { this.departureEstimated = departureEstimated; }

    public LocalDateTime getArrivalActual() { return arrivalActual; }
    public void setArrivalActual(LocalDateTime arrivalActual) { this.arrivalActual = arrivalActual; }

    public LocalDateTime getDepartureActual() { return departureActual; }
    public void setDepartureActual(LocalDateTime departureActual) { this.departureActual = departureActual; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
