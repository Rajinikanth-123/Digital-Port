package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user; // Nullable if system broadcast

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String type = "SYSTEM"; // SHIPMENT, VESSEL, BERTH, CUSTOMS, INVOICE, PAYMENT, SYSTEM

    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(nullable = false)
    private Boolean archived = false;

    @Column(nullable = false, length = 20)
    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT

    @Column(name = "entity_name", length = 80)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {}

    public Notification(User user, String title, String message, String type, String entityName, Long entityId) {
        this(user, title, message, type, entityName, entityId, "NORMAL");
    }

    public Notification(User user, String title, String message, String type, String entityName, Long entityId, String priority) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type != null ? type : "SYSTEM";
        this.entityName = entityName;
        this.entityId = entityId;
        this.priority = priority != null ? priority : "NORMAL";
        this.readStatus = false;
        this.archived = false;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.readStatus == null) {
            this.readStatus = false;
        }
        if (this.archived == null) {
            this.archived = false;
        }
        if (this.priority == null) {
            this.priority = "NORMAL";
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getReadStatus() { return readStatus; }
    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
        if (Boolean.TRUE.equals(readStatus) && this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
