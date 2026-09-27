package com.portshipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.AuditLog;
import com.portshipping.entity.User;
import com.portshipping.entity.Vessel;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class VesselService {

    private final VesselRepository vesselRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShippingScheduleRepository scheduleRepository;
    private final BerthAllocationRepository allocationRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public VesselService(VesselRepository vesselRepository,
                         ShipmentRepository shipmentRepository,
                         ShippingScheduleRepository scheduleRepository,
                         BerthAllocationRepository allocationRepository,
                         AuditLogRepository auditLogRepository,
                         NotificationService notificationService,
                         ObjectMapper objectMapper) {
        this.vesselRepository = vesselRepository;
        this.shipmentRepository = shipmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.allocationRepository = allocationRepository;
        this.auditLogRepository = auditLogRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<Vessel> getVessels(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String q = search.trim();
            return vesselRepository.findByNameContainingIgnoreCaseOrImoNumberContainingIgnoreCaseOrVesselTypeContainingIgnoreCase(q, q, q, pageable);
        }
        return vesselRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Vessel getVessel(Long id) {
        return vesselRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vessel not found with ID: " + id));
    }

    public Vessel createVessel(String name, String imoNumber, String vesselType, String flag, Integer capacity, String operatorName, Double length, Double draft, String status) {
        String cleanImo = imoNumber.trim();
        if (vesselRepository.existsByImoNumberIgnoreCase(cleanImo)) {
            throw new ConflictException("IMO Number '" + cleanImo + "' is already in use by another vessel.");
        }

        validateSpecifications(capacity, length, draft);

        Vessel vessel = new Vessel(
                name.trim(),
                cleanImo,
                vesselType != null && !vesselType.isBlank() ? vesselType.trim() : "CONTAINER",
                flag != null ? flag.trim() : "",
                capacity != null ? capacity : 0,
                operatorName != null ? operatorName.trim() : "",
                length,
                draft,
                status != null && !status.isBlank() ? status.trim() : "ACTIVE"
        );
        return vesselRepository.save(vessel);
    }

    public Vessel updateVessel(Long id, String name, String imoNumber, String vesselType, String flag, Integer capacity, String operatorName, Double length, Double draft, String status) {
        Vessel vessel = getVessel(id);

        String cleanImo = imoNumber.trim();
        if (vesselRepository.existsByImoNumberIgnoreCaseAndIdNot(cleanImo, id)) {
            throw new ConflictException("IMO Number '" + cleanImo + "' is already in use by another vessel.");
        }

        validateSpecifications(capacity, length, draft);

        vessel.setName(name.trim());
        vessel.setImoNumber(cleanImo);
        if (vesselType != null && !vesselType.isBlank()) vessel.setVesselType(vesselType.trim());
        if (flag != null) vessel.setFlag(flag.trim());
        if (capacity != null) vessel.setCapacity(capacity);
        if (operatorName != null) vessel.setOperatorName(operatorName.trim());
        vessel.setLength(length);
        vessel.setDraft(draft);
        if (status != null && !status.isBlank()) vessel.setStatus(status.trim());

        return vesselRepository.save(vessel);
    }

    public Vessel updateVesselTracking(Long id, Double latitude, Double longitude, String currentLocation, String status, User currentUser) {
        Vessel vessel = getVessel(id);

        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude must be between -90.0 and 90.0 degrees.");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitude must be between -180.0 and 180.0 degrees.");
        }

        String oldStatus = vessel.getStatus();
        String oldLocation = vessel.getCurrentLocation();

        if (latitude != null) vessel.setLatitude(latitude);
        if (longitude != null) vessel.setLongitude(longitude);
        if (currentLocation != null && !currentLocation.isBlank()) vessel.setCurrentLocation(currentLocation.trim());
        if (status != null && !status.isBlank()) vessel.setStatus(status.trim());
        vessel.setLastTrackedAt(Instant.now());

        Vessel updated = vesselRepository.save(vessel);

        // Record historical tracking timeline event in audit_logs
        recordTrackingAuditLog(currentUser, updated.getId(), Map.of(
                "action", "VESSEL_TRACKING_UPDATE",
                "vesselName", updated.getName(),
                "latitude", updated.getLatitude() != null ? updated.getLatitude() : "N/A",
                "longitude", updated.getLongitude() != null ? updated.getLongitude() : "N/A",
                "location", updated.getCurrentLocation() != null ? updated.getCurrentLocation() : "Unknown",
                "status", updated.getStatus()
        ));

        // Trigger notification on status/location update
        if ((status != null && !status.equalsIgnoreCase(oldStatus)) || (currentLocation != null && !currentLocation.equalsIgnoreCase(oldLocation))) {
            notificationService.sendNotification(null,
                    "Vessel Location & Status Update",
                    "Vessel '" + updated.getName() + "' position updated to: " +
                            (updated.getCurrentLocation() != null ? updated.getCurrentLocation() : "Sea") +
                            " (Status: " + updated.getStatus() + ")",
                    "VESSEL", "Vessel", updated.getId());
        }

        return updated;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getVesselTrackingHistory(Long id) {
        getVessel(id); // Ensure vessel exists
        return auditLogRepository.findByEntityNameIgnoreCaseAndEntityIdOrderByCreatedAtDesc("VESSEL", id);
    }

    public void deleteVessel(Long id) {
        Vessel vessel = getVessel(id);

        long shipments = shipmentRepository.countByVesselId(id);
        long schedules = scheduleRepository.countByVesselId(id);
        long allocations = allocationRepository.countByVesselId(id);

        if (shipments > 0 || schedules > 0 || allocations > 0) {
            throw new ConflictException("Cannot delete Vessel '" + vessel.getName() + "': Referenced by " +
                    shipments + " shipment(s), " + schedules + " schedule(s), and " + allocations + " berth allocation(s).");
        }

        vesselRepository.delete(vessel);
    }

    private void validateSpecifications(Integer capacity, Double length, Double draft) {
        if (capacity != null && capacity < 0) {
            throw new IllegalArgumentException("Vessel capacity cannot be negative.");
        }
        if (length != null && length <= 0) {
            throw new IllegalArgumentException("Vessel length must be a positive value.");
        }
        if (draft != null && draft <= 0) {
            throw new IllegalArgumentException("Vessel draft must be a positive value.");
        }
    }

    private void recordTrackingAuditLog(User user, Long entityId, Map<String, Object> detailsMap) {
        try {
            String detailsJson = objectMapper.writeValueAsString(detailsMap);
            AuditLog log = new AuditLog(user, "VESSEL_TRACKING_UPDATE", "VESSEL", entityId, detailsJson);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Non-blocking fallback
        }
    }
}
