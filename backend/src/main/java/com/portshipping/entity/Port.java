package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ports", uniqueConstraints = @UniqueConstraint(name = "uq_ports_code", columnNames = "code"))
public class Port {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 80)
    private String country;

    @Column(nullable = false, length = 60)
    private String timezone = "UTC";

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected Port() {}

    public Port(String name, String code, String country) {
        this.name = name;
        this.code = code;
        this.country = country;
    }

    public Port(String name, String code, String country, String timezone, Double latitude, Double longitude) {
        this.name = name;
        this.code = code;
        this.country = country;
        this.timezone = timezone != null && !timezone.isBlank() ? timezone : "UTC";
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
