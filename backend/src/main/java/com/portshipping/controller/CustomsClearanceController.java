package com.portshipping.controller;

import com.portshipping.entity.CustomsClearance;
import com.portshipping.service.CustomsClearanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customs-clearances")
public class CustomsClearanceController {

    private final CustomsClearanceService customsService;

    public CustomsClearanceController(CustomsClearanceService customsService) {
        this.customsService = customsService;
    }

    @GetMapping
    public Page<CustomsClearance> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long shipmentId) {
        return customsService.searchDeclarations(search, status, shipmentId, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomsClearance> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customsService.getById(id));
    }

    public static class CreateDeclarationRequest {
        @NotNull(message = "Shipment ID is required")
        private Long shipmentId;

        @NotBlank(message = "Declaration number is required")
        private String declarationNumber;

        private String status;
        private String documentDetails;

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

        public String getDeclarationNumber() { return declarationNumber; }
        public void setDeclarationNumber(String declarationNumber) { this.declarationNumber = declarationNumber; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getDocumentDetails() { return documentDetails; }
        public void setDocumentDetails(String documentDetails) { this.documentDetails = documentDetails; }
    }

    @PostMapping
    public ResponseEntity<CustomsClearance> fileDeclaration(@Valid @RequestBody CreateDeclarationRequest req, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        CustomsClearance created = customsService.fileDeclaration(
                req.getShipmentId(),
                req.getDeclarationNumber(),
                req.getStatus(),
                req.getDocumentDetails(),
                username
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public static class UpdateStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;

        private String remarks;
        private Long assignedOfficerId;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }

        public Long getAssignedOfficerId() { return assignedOfficerId; }
        public void setAssignedOfficerId(Long assignedOfficerId) { this.assignedOfficerId = assignedOfficerId; }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CustomsClearance> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        CustomsClearance updated = customsService.updateStatus(
                id,
                req.getStatus(),
                req.getRemarks(),
                req.getAssignedOfficerId(),
                username
        );
        return ResponseEntity.ok(updated);
    }

    public static class UpdateDeclarationRequest {
        private String declarationNumber;
        private Long shipmentId;
        private String status;
        private String documentDetails;
        private String remarks;

        public String getDeclarationNumber() { return declarationNumber; }
        public void setDeclarationNumber(String declarationNumber) { this.declarationNumber = declarationNumber; }

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getDocumentDetails() { return documentDetails; }
        public void setDocumentDetails(String documentDetails) { this.documentDetails = documentDetails; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomsClearance> updateDeclaration(
            @PathVariable Long id,
            @RequestBody UpdateDeclarationRequest req,
            Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        CustomsClearance updated = customsService.updateDeclaration(
                id,
                req.getDeclarationNumber(),
                req.getShipmentId(),
                req.getStatus(),
                req.getDocumentDetails(),
                req.getRemarks(),
                username
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        customsService.deleteDeclaration(id, username);
        return ResponseEntity.noContent().build();
    }
}
