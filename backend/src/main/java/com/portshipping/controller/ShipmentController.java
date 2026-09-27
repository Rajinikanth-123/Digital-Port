package com.portshipping.controller;

import com.portshipping.entity.AuditLog;
import com.portshipping.entity.Shipment;
import com.portshipping.service.ShipmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public ResponseEntity<?> getShipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        if (size >= 100) {
            List<Shipment> list = shipmentService.getAllShipmentsList();
            return ResponseEntity.ok(list);
        }
        Page<Shipment> paged = shipmentService.getAllShipments(page, size, search, status);
        return ResponseEntity.ok(paged);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<List<AuditLog>> getShipmentTrackingHistory(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentTrackingHistory(id));
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@Valid @RequestBody ShipmentRequest req) {
        Shipment created = shipmentService.createShipment(
                req.getTrackingNumber(),
                req.getReferenceNumber(),
                req.getOrigin(),
                req.getDestination(),
                req.getShipper(),
                req.getConsignee(),
                req.getEstimatedArrival(),
                req.getStatus(),
                req.getContainerCount(),
                req.getVesselId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shipment> updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentRequest req) {
        Shipment updated = shipmentService.updateShipment(
                id,
                req.getReferenceNumber() != null ? req.getReferenceNumber() : req.getTrackingNumber(),
                req.getOrigin(),
                req.getDestination(),
                req.getShipper(),
                req.getConsignee(),
                req.getEstimatedArrival(),
                req.getStatus(),
                req.getContainerCount(),
                req.getVesselId()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable Long id) {
        shipmentService.deleteShipment(id);
        return ResponseEntity.noContent().build();
    }

    public static class ShipmentRequest {
        private String trackingNumber;
        private String referenceNumber;

        @NotBlank(message = "Origin port/location is required")
        private String origin;

        @NotBlank(message = "Destination port/location is required")
        private String destination;

        private String shipper;
        private String consignee;
        private LocalDate estimatedArrival;
        private String status;

        @Min(value = 0, message = "Container count cannot be negative")
        private Integer containerCount;
        private Long vesselId;

        public String getTrackingNumber() { return trackingNumber; }
        public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

        public String getOrigin() { return origin; }
        public void setOrigin(String origin) { this.origin = origin; }

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }

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

        public Long getVesselId() { return vesselId; }
        public void setVesselId(Long vesselId) { this.vesselId = vesselId; }
    }
}
