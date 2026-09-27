package com.portshipping.service;

import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final VesselRepository vesselRepository;
    private final ContainerRepository containerRepository;
    private final CargoRepository cargoRepository;
    private final CustomsClearanceRepository customsClearanceRepository;
    private final DocumentRepository documentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           VesselRepository vesselRepository,
                           ContainerRepository containerRepository,
                           CargoRepository cargoRepository,
                           CustomsClearanceRepository customsClearanceRepository,
                           DocumentRepository documentRepository,
                           InvoiceRepository invoiceRepository,
                           AuditLogRepository auditLogRepository,
                           AuditLogService auditLogService,
                           NotificationService notificationService) {
        this.shipmentRepository = shipmentRepository;
        this.vesselRepository = vesselRepository;
        this.containerRepository = containerRepository;
        this.cargoRepository = cargoRepository;
        this.customsClearanceRepository = customsClearanceRepository;
        this.documentRepository = documentRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<Shipment> getAllShipments(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return shipmentRepository.searchShipments(search, status, pageable);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getAllShipmentsList() {
        return shipmentRepository.findAll(Sort.by("id").descending());
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getShipmentTrackingHistory(Long shipmentId) {
        getShipmentById(shipmentId); // Ensure shipment exists
        return auditLogRepository.findByEntityNameIgnoreCaseAndEntityIdOrderByCreatedAtDesc("SHIPMENT", shipmentId);
    }

    public Shipment createShipment(String trackingNumber, String referenceNumber, String origin, String destination,
                                   String shipper, String consignee, LocalDate estimatedArrival, String status,
                                   Integer containerCount, Long vesselId) {
        if (origin == null || origin.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Origin port/location is required.");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination port/location is required.");
        }

        String finalTracking = (trackingNumber != null && !trackingNumber.trim().isEmpty())
                ? trackingNumber.trim()
                : "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String finalRef = (referenceNumber != null && !referenceNumber.trim().isEmpty())
                ? referenceNumber.trim()
                : finalTracking;

        if (shipmentRepository.existsByTrackingNumberIgnoreCase(finalTracking)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tracking number '" + finalTracking + "' already exists.");
        }
        if (shipmentRepository.existsByReferenceNumberIgnoreCase(finalRef)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference number '" + finalRef + "' already exists.");
        }

        if (containerCount != null && containerCount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container count cannot be negative.");
        }

        Vessel vessel = null;
        if (vesselId != null) {
            vessel = vesselRepository.findById(vesselId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vessel ID: " + vesselId));
        }

        Shipment shipment = new Shipment(finalTracking, finalRef, origin.trim(), destination.trim(), shipper, consignee,
                estimatedArrival, status != null ? status.trim().toUpperCase() : "REGISTERED",
                containerCount != null ? containerCount : 0, vessel);

        Shipment saved = shipmentRepository.save(shipment);

        // Record tracking audit log entry
        auditLogService.logSuccess(null, "SHIPMENT_CREATED", "Shipment", saved.getId(),
                "Registered shipment #" + saved.getTrackingNumber() + " (" + saved.getOrigin() + " -> " + saved.getDestination() + ")");

        // Trigger notification
        notificationService.notifyUsersByRole(
                List.of("ADMIN", "PORT_MANAGER", "SHIPPING_AGENT"),
                "New Shipment Created: " + saved.getTrackingNumber(),
                "Shipment #" + saved.getTrackingNumber() + " (" + saved.getOrigin() + " -> " + saved.getDestination() + ") has been registered.",
                "SHIPMENT", "Shipment", saved.getId(), "NORMAL"
        );

        return saved;
    }

    public Shipment updateShipment(Long id, String referenceNumber, String origin, String destination,
                                   String shipper, String consignee, LocalDate estimatedArrival, String status,
                                   Integer containerCount, Long vesselId) {
        Shipment shipment = getShipmentById(id);
        String oldStatus = shipment.getStatus();

        if (referenceNumber != null && !referenceNumber.trim().isEmpty()) {
            String finalRef = referenceNumber.trim();
            if (shipmentRepository.existsByReferenceNumberIgnoreCaseAndIdNot(finalRef, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reference number '" + finalRef + "' already exists.");
            }
            shipment.setReferenceNumber(finalRef);
        }

        if (origin != null && !origin.trim().isEmpty()) {
            shipment.setOrigin(origin.trim());
        }

        if (destination != null && !destination.trim().isEmpty()) {
            shipment.setDestination(destination.trim());
        }

        if (shipper != null) shipment.setShipper(shipper.trim());
        if (consignee != null) shipment.setConsignee(consignee.trim());
        if (estimatedArrival != null) shipment.setEstimatedArrival(estimatedArrival);

        boolean statusChanged = false;
        if (status != null && !status.trim().isEmpty()) {
            String newStatus = status.trim().toUpperCase();
            if (!newStatus.equals(oldStatus)) {
                shipment.setStatus(newStatus);
                statusChanged = true;
            }
        }

        if (containerCount != null) {
            if (containerCount < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container count cannot be negative.");
            }
            shipment.setContainerCount(containerCount);
        }

        if (vesselId != null) {
            Vessel vessel = vesselRepository.findById(vesselId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vessel ID: " + vesselId));
            shipment.setVessel(vessel);
        } else {
            shipment.setVessel(null);
        }

        Shipment updated = shipmentRepository.save(shipment);

        if (statusChanged) {
            auditLogService.logChange(null, "SHIPMENT_STATUS_UPDATED", "Shipment", updated.getId(),
                    Map.of("status", oldStatus), Map.of("status", updated.getStatus()),
                    "Shipment #" + updated.getTrackingNumber() + " status updated from " + oldStatus + " to " + updated.getStatus());

            notificationService.notifyUsersByRole(
                    List.of("ADMIN", "PORT_MANAGER", "SHIPPING_AGENT"),
                    "Shipment Status Updated: " + updated.getTrackingNumber(),
                    "Shipment #" + updated.getTrackingNumber() + " status changed to " + updated.getStatus() + ".",
                    "SHIPMENT", "Shipment", updated.getId(), "NORMAL"
            );
        }

        return updated;
    }

    public void deleteShipment(Long id) {
        Shipment shipment = getShipmentById(id);

        long containers = containerRepository.countByShipmentId(id);
        if (containers > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Shipment #" + id + ": linked to " + containers + " container(s).");
        }

        long cargoCount = cargoRepository.countByShipmentId(id);
        if (cargoCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Shipment #" + id + ": linked to " + cargoCount + " cargo item(s).");
        }

        long customs = customsClearanceRepository.countByShipmentId(id);
        if (customs > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Shipment #" + id + ": linked to " + customs + " customs clearance(s).");
        }

        long docs = documentRepository.countByShipmentId(id);
        if (docs > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Shipment #" + id + ": linked to " + docs + " document(s).");
        }

        long invoices = invoiceRepository.countByShipmentId(id);
        if (invoices > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Shipment #" + id + ": linked to " + invoices + " invoice(s).");
        }

        shipmentRepository.delete(shipment);
    }
}
