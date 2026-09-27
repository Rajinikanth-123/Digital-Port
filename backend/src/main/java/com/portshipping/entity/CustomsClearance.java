package com.portshipping.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "customs_clearances", uniqueConstraints = @UniqueConstraint(name = "uq_customs_declaration", columnNames = "declaration_number"))
public class CustomsClearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "declaration_number", nullable = false, length = 60)
    private String declarationNumber;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Lob
    @Column(name = "document_details", columnDefinition = "TEXT")
    private String documentDetails;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @Lob
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public CustomsClearance() {}

    public CustomsClearance(Shipment shipment, String declarationNumber, String status, String documentDetails) {
        this.shipment = shipment;
        this.declarationNumber = declarationNumber;
        this.status = status != null ? status : "PENDING";
        this.documentDetails = documentDetails;
        this.submittedAt = LocalDateTime.now();
    }

    public CustomsClearance(Shipment shipment, String declarationNumber, String status, String documentDetails, User assignedOfficer, String remarks) {
        this(shipment, declarationNumber, status, documentDetails);
        this.assignedOfficer = assignedOfficer;
        this.remarks = remarks;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (submittedAt == null) submittedAt = LocalDateTime.now();
        if ("APPROVED".equalsIgnoreCase(status) || "CLEARED".equalsIgnoreCase(status)) {
            if (clearedAt == null) clearedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if ("APPROVED".equalsIgnoreCase(status) || "CLEARED".equalsIgnoreCase(status)) {
            if (clearedAt == null) clearedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getDeclarationNumber() { return declarationNumber; }
    public void setDeclarationNumber(String declarationNumber) { this.declarationNumber = declarationNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDocumentDetails() { return documentDetails; }
    public void setDocumentDetails(String documentDetails) { this.documentDetails = documentDetails; }

    public User getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(User assignedOfficer) { this.assignedOfficer = assignedOfficer; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getClearedAt() { return clearedAt; }
    public void setClearedAt(LocalDateTime clearedAt) { this.clearedAt = clearedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
