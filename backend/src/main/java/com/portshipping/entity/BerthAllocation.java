package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "berth_allocations")
public class BerthAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80)
    private String berth;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "berth_id")
    private Berth berthEntity;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vessel_id")
    private Vessel vessel;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(nullable = false, length = 30)
    private String status = "SCHEDULED";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    protected BerthAllocation() {}

    public BerthAllocation(String berth, Instant startTime, Instant endTime, Vessel vessel) {
        this.berth = berth;
        this.startTime = startTime;
        this.endTime = endTime;
        this.vessel = vessel;
    }

    public BerthAllocation(Berth berthEntity, String berth, Instant startTime, Instant endTime, Vessel vessel, String status) {
        this.berthEntity = berthEntity;
        this.berth = berth != null ? berth : (berthEntity != null ? berthEntity.getName() : "");
        this.startTime = startTime;
        this.endTime = endTime;
        this.vessel = vessel;
        this.status = status != null && !status.isBlank() ? status : "SCHEDULED";
    }

    public Long getId() { return id; }

    public String getBerth() { return berth; }
    public void setBerth(String berth) { this.berth = berth; }

    public Berth getBerthEntity() { return berthEntity; }
    public void setBerthEntity(Berth berthEntity) { this.berthEntity = berthEntity; }

    public Vessel getVessel() { return vessel; }
    public void setVessel(Vessel vessel) { this.vessel = vessel; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
