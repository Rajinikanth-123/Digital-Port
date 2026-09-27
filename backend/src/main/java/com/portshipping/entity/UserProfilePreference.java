package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_preferences")
public class UserProfilePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 20)
    private String theme = "light";

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(nullable = false, length = 60)
    private String timezone = "UTC";

    @Column(name = "notify_shipments", nullable = false)
    private boolean notifyShipments = true;

    @Column(name = "notify_vessels", nullable = false)
    private boolean notifyVessels = true;

    @Column(name = "notify_customs", nullable = false)
    private boolean notifyCustoms = true;

    @Column(name = "notify_invoices", nullable = false)
    private boolean notifyInvoices = true;

    @Column(name = "notify_system", nullable = false)
    private boolean notifySystem = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UserProfilePreference() {}

    public UserProfilePreference(User user) {
        this.user = user;
        this.theme = "light";
        this.language = "en";
        this.timezone = "UTC";
        this.notifyShipments = true;
        this.notifyVessels = true;
        this.notifyCustoms = true;
        this.notifyInvoices = true;
        this.notifySystem = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme != null ? theme : "light"; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language != null ? language : "en"; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone != null ? timezone : "UTC"; }

    public boolean isNotifyShipments() { return notifyShipments; }
    public void setNotifyShipments(boolean notifyShipments) { this.notifyShipments = notifyShipments; }

    public boolean isNotifyVessels() { return notifyVessels; }
    public void setNotifyVessels(boolean notifyVessels) { this.notifyVessels = notifyVessels; }

    public boolean isNotifyCustoms() { return notifyCustoms; }
    public void setNotifyCustoms(boolean notifyCustoms) { this.notifyCustoms = notifyCustoms; }

    public boolean isNotifyInvoices() { return notifyInvoices; }
    public void setNotifyInvoices(boolean notifyInvoices) { this.notifyInvoices = notifyInvoices; }

    public boolean isNotifySystem() { return notifySystem; }
    public void setNotifySystem(boolean notifySystem) { this.notifySystem = notifySystem; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
