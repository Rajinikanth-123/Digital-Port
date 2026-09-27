package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_name", columnList = "entity_name"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_outcome", columnList = "outcome")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "actor_name", length = 120)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_name", nullable = false, length = 80)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(length = 20, nullable = false)
    private String outcome = "SUCCESS"; // SUCCESS or FAILURE

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "json")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {}

    public AuditLog(User user, String action, String entityName, Long entityId, String details) {
        this.user = user;
        this.actorName = user != null ? user.getName() : "System";
        this.actorRole = user != null ? user.getRole() : "SYSTEM";
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.details = details;
        this.description = details;
        this.outcome = "SUCCESS";
        this.createdAt = Instant.now();
    }

    public AuditLog(User user, String actorName, String actorRole, String action, String entityName, Long entityId, String outcome, String ipAddress, String description, String details) {
        this.user = user;
        this.actorName = actorName != null ? actorName : (user != null ? user.getName() : "System");
        this.actorRole = actorRole != null ? actorRole : (user != null ? user.getRole() : "SYSTEM");
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.outcome = outcome != null ? outcome : "SUCCESS";
        this.ipAddress = ipAddress;
        this.description = description;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
