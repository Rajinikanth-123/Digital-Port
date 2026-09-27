package com.portshipping.service;

import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class CustomsClearanceService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "IN_REVIEW", "APPROVED", "CLEARED", "REJECTED", "ADDITIONAL_INFO_REQUIRED", "CANCELLED"
    );

    private final CustomsClearanceRepository customsRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;

    public CustomsClearanceService(CustomsClearanceRepository customsRepository,
                                  ShipmentRepository shipmentRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  AuditLogRepository auditLogRepository) {
        this.customsRepository = customsRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<CustomsClearance> searchDeclarations(String search, String status, Long shipmentId, Pageable pageable) {
        if (shipmentId != null) {
            List<CustomsClearance> list = customsRepository.findByShipmentId(shipmentId);
            return new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return customsRepository.findByStatusIgnoreCase(status.trim(), pageable);
        }
        if (search != null && !search.isBlank()) {
            return customsRepository.findByDeclarationNumberContainingIgnoreCaseOrStatusContainingIgnoreCase(search.trim(), search.trim(), pageable);
        }
        return customsRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public CustomsClearance getById(Long id) {
        return customsRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customs declaration #" + id + " not found"));
    }

    public CustomsClearance fileDeclaration(Long shipmentId, String declarationNumber, String status, String documentDetails, String username) {
        if (declarationNumber == null || declarationNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declaration number is required.");
        }

        String declClean = declarationNumber.trim().toUpperCase();
        if (customsRepository.existsByDeclarationNumberIgnoreCase(declClean)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customs declaration number '" + declClean + "' already exists.");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Linked Shipment #" + shipmentId + " not found."));

        String finalStatus = (status != null && VALID_STATUSES.contains(status.toUpperCase())) ? status.toUpperCase() : "PENDING";
        CustomsClearance clearance = new CustomsClearance(shipment, declClean, finalStatus, documentDetails);

        User activeUser = findUserByEmail(username);
        if (activeUser != null) clearance.setAssignedOfficer(activeUser);

        CustomsClearance saved = customsRepository.save(clearance);

        // Audit log & notification
        auditLogRepository.save(new AuditLog(activeUser, "CREATE_CUSTOMS_DECLARATION", "CustomsClearance", saved.getId(),
                "{\"declarationNumber\":\"" + declClean + "\",\"shipmentId\":" + shipmentId + "}"));

        if (activeUser != null) {
            notificationService.sendNotification(activeUser, "Customs Filing Registered",
                    "Customs declaration " + declClean + " filed for Shipment " + shipment.getTrackingNumber(),
                    "CUSTOMS", "CustomsClearance", saved.getId());
        }

        return saved;
    }

    public CustomsClearance updateStatus(Long id, String status, String remarks, Long officerId, String username) {
        CustomsClearance clearance = getById(id);

        if (status == null || !VALID_STATUSES.contains(status.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status '" + status + "'. Valid options: " + VALID_STATUSES);
        }

        String newStatus = status.toUpperCase();
        String oldStatus = clearance.getStatus();

        // Enforce remarks for REJECTED and ADDITIONAL_INFO_REQUIRED
        if (("REJECTED".equals(newStatus) || "ADDITIONAL_INFO_REQUIRED".equals(newStatus)) && (remarks == null || remarks.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason/remarks string is mandatory when status is set to " + newStatus);
        }

        // Prevent invalid status transition (e.g. APPROVED cannot be rejected directly unless admin)
        if ("APPROVED".equalsIgnoreCase(oldStatus) && "PENDING".equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approved declarations cannot be reverted to PENDING status.");
        }

        clearance.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) clearance.setRemarks(remarks.trim());

        if ("APPROVED".equals(newStatus) || "CLEARED".equals(newStatus)) {
            clearance.setClearedAt(LocalDateTime.now());
        }

        User officer = null;
        if (officerId != null) {
            officer = userRepository.findById(officerId).orElse(null);
        }
        if (officer == null) {
            officer = findUserByEmail(username);
        }
        if (officer != null) clearance.setAssignedOfficer(officer);

        CustomsClearance updated = customsRepository.save(clearance);

        User activeUser = findUserByEmail(username);
        auditLogRepository.save(new AuditLog(activeUser, "UPDATE_CUSTOMS_STATUS", "CustomsClearance", updated.getId(),
                "{\"oldStatus\":\"" + oldStatus + "\",\"newStatus\":\"" + newStatus + "\",\"declarationNumber\":\"" + updated.getDeclarationNumber() + "\"}"));

        if (activeUser != null) {
            notificationService.sendNotification(activeUser, "Customs Status Update",
                    "Declaration " + updated.getDeclarationNumber() + " changed status to " + newStatus,
                    "CUSTOMS", "CustomsClearance", updated.getId());
        }

        return updated;
    }

    public CustomsClearance updateDeclaration(Long id, String declarationNumber, Long shipmentId, String status, String documentDetails, String remarks, String username) {
        CustomsClearance clearance = getById(id);

        if (declarationNumber != null && !declarationNumber.isBlank()) {
            String declClean = declarationNumber.trim().toUpperCase();
            if (customsRepository.existsByDeclarationNumberIgnoreCaseAndIdNot(declClean, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Declaration number '" + declClean + "' is already in use by another record.");
            }
            clearance.setDeclarationNumber(declClean);
        }

        if (shipmentId != null) {
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment #" + shipmentId + " not found."));
            clearance.setShipment(shipment);
        }

        if (status != null && VALID_STATUSES.contains(status.toUpperCase())) {
            clearance.setStatus(status.toUpperCase());
        }

        if (documentDetails != null) clearance.setDocumentDetails(documentDetails.trim());
        if (remarks != null) clearance.setRemarks(remarks.trim());

        return customsRepository.save(clearance);
    }

    public void deleteDeclaration(Long id, String username) {
        CustomsClearance clearance = getById(id);
        customsRepository.delete(clearance);

        User activeUser = findUserByEmail(username);
        auditLogRepository.save(new AuditLog(activeUser, "DELETE_CUSTOMS_DECLARATION", "CustomsClearance", id,
                "{\"declarationNumber\":\"" + clearance.getDeclarationNumber() + "\"}"));
    }

    private User findUserByEmail(String username) {
        if (username == null) return null;
        return userRepository.findByEmailIgnoreCase(username).orElse(null);
    }
}
