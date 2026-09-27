package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vessels", uniqueConstraints = @UniqueConstraint(name = "uq_vessels_imo", columnNames = "imo_number"))
public class Vessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "imo_number", nullable = false, length = 20)
    private String imoNumber;

    @Column(name = "vessel_type", nullable = false, length = 60)
    private String vesselType;

    @Column(nullable = false, length = 80)
    private String flag = "";

    @Column(nullable = false)
    private Integer capacity = 0;

    @Column(name = "operator_name", nullable = false, length = 160)
    private String operatorName = "";

    @Column(name = "length_m")
    private Double length;

    @Column(name = "draft_m")
    private Double draft;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "current_location", length = 120)
    private String currentLocation;

    @Column(name = "last_tracked_at")
    private Instant lastTrackedAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected Vessel() {}

    public Vessel(String name, String imoNumber, String vesselType, Double length, Double draft) {
        this.name = name;
        this.imoNumber = imoNumber;
        this.vesselType = vesselType;
        this.length = length;
        this.draft = draft;
    }

    public Vessel(String name, String imoNumber, String vesselType, String flag, Integer capacity, String operatorName, Double length, Double draft, String status) {
        this.name = name;
        this.imoNumber = imoNumber;
        this.vesselType = vesselType;
        this.flag = flag != null ? flag : "";
        this.capacity = capacity != null ? capacity : 0;
        this.operatorName = operatorName != null ? operatorName : "";
        this.length = length;
        this.draft = draft;
        this.status = status != null && !status.isBlank() ? status : "ACTIVE";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImoNumber() { return imoNumber; }
    public void setImoNumber(String imoNumber) { this.imoNumber = imoNumber; }

    public String getVesselType() { return vesselType; }
    public void setVesselType(String vesselType) { this.vesselType = vesselType; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Double getLength() { return length; }
    public void setLength(Double length) { this.length = length; }

    public Double getDraft() { return draft; }
    public void setDraft(Double draft) { this.draft = draft; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public Instant getLastTrackedAt() { return lastTrackedAt; }
    public void setLastTrackedAt(Instant lastTrackedAt) { this.lastTrackedAt = lastTrackedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
