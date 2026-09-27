package com.portshipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.*;
import com.portshipping.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ShippingScheduleService {

    private final ShippingScheduleRepository scheduleRepository;
    private final VesselRepository vesselRepository;
    private final BerthRepository berthRepository;
    private final BerthAllocationRepository allocationRepository;
    private final NotificationService notificationService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public ShippingScheduleService(ShippingScheduleRepository scheduleRepository,
                                   VesselRepository vesselRepository,
                                   BerthRepository berthRepository,
                                   BerthAllocationRepository allocationRepository,
                                   NotificationService notificationService,
                                   AuditLogRepository auditLogRepository,
                                   ObjectMapper objectMapper) {
        this.scheduleRepository = scheduleRepository;
        this.vesselRepository = vesselRepository;
        this.berthRepository = berthRepository;
        this.allocationRepository = allocationRepository;
        this.notificationService = notificationService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<ShippingSchedule> getSchedules(String search, String status, Long vesselId, Long berthId, Pageable pageable) {
        return scheduleRepository.searchSchedules(search, status, vesselId, berthId, pageable);
    }

    @Transactional(readOnly = true)
    public ShippingSchedule getScheduleById(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found: #" + id));
    }

    public ShippingSchedule createSchedule(Long vesselId, Long berthId, LocalDateTime arrivalEstimated, LocalDateTime departureEstimated, String status, User currentUser) {
        if (vesselId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vessel ID is required.");
        }
        Vessel vessel = vesselRepository.findById(vesselId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vessel ID: " + vesselId));

        Berth berth = null;
        if (berthId != null) {
            berth = berthRepository.findById(berthId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid berth ID: " + berthId));
        }

        validateTimes(arrivalEstimated, departureEstimated);
        validateConflicts(vessel.getId(), berth != null ? berth.getId() : null, arrivalEstimated, departureEstimated, null);

        ShippingSchedule schedule = new ShippingSchedule(vessel, berth, arrivalEstimated, departureEstimated, status);
        ShippingSchedule saved = scheduleRepository.save(schedule);

        recordAuditLog(currentUser, "SCHEDULE_CREATED", saved.getId(),
                Map.of("vessel", vessel.getName(), "status", saved.getStatus(), "arrival", saved.getArrivalEstimated().toString()));

        notificationService.sendNotification(null,
                "New Schedule Created",
                "Vessel '" + vessel.getName() + "' scheduled to arrive on " + saved.getArrivalEstimated().toString().replace("T", " "),
                "SCHEDULE", "ShippingSchedule", saved.getId());

        return saved;
    }

    public ShippingSchedule updateSchedule(Long id, Long vesselId, Long berthId, LocalDateTime arrivalEstimated, LocalDateTime departureEstimated, String status, User currentUser) {
        ShippingSchedule existing = getScheduleById(id);

        Vessel vessel = existing.getVessel();
        if (vesselId != null && !vesselId.equals(vessel.getId())) {
            vessel = vesselRepository.findById(vesselId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid vessel ID: " + vesselId));
        }

        Berth berth = existing.getBerth();
        if (berthId != null) {
            berth = berthRepository.findById(berthId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid berth ID: " + berthId));
        } else {
            berth = null;
        }

        LocalDateTime eta = arrivalEstimated != null ? arrivalEstimated : existing.getArrivalEstimated();
        LocalDateTime etd = departureEstimated != null ? departureEstimated : existing.getDepartureEstimated();

        validateTimes(eta, etd);
        validateConflicts(vessel.getId(), berth != null ? berth.getId() : null, eta, etd, id);

        String oldStatus = existing.getStatus();
        existing.setVessel(vessel);
        existing.setBerth(berth);
        existing.setArrivalEstimated(eta);
        existing.setDepartureEstimated(etd);
        if (status != null && !status.isBlank()) {
            existing.setStatus(status.toUpperCase());
        }

        ShippingSchedule updated = scheduleRepository.save(existing);

        recordAuditLog(currentUser, "SCHEDULE_UPDATED", updated.getId(),
                Map.of("vessel", vessel.getName(), "oldStatus", oldStatus, "newStatus", updated.getStatus()));

        if (!oldStatus.equalsIgnoreCase(updated.getStatus())) {
            notificationService.sendNotification(null,
                    "Schedule Status Changed",
                    "Vessel '" + vessel.getName() + "' schedule status changed from " + oldStatus + " to " + updated.getStatus(),
                    "SCHEDULE", "ShippingSchedule", updated.getId());
        }

        return updated;
    }

    public void deleteSchedule(Long id, User currentUser) {
        ShippingSchedule existing = getScheduleById(id);
        String vesselName = existing.getVessel() != null ? existing.getVessel().getName() : "Unknown";

        scheduleRepository.delete(existing);

        recordAuditLog(currentUser, "SCHEDULE_DELETED", id, Map.of("vessel", vesselName));
        notificationService.sendNotification(null, "Schedule Cancelled", "Schedule #" + id + " for vessel '" + vesselName + "' has been deleted.", "SCHEDULE", "ShippingSchedule", id);
    }

    private void validateTimes(LocalDateTime eta, LocalDateTime etd) {
        if (eta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estimated arrival time is required.");
        }
        if (etd != null && (etd.isBefore(eta) || etd.isEqual(eta))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estimated departure time must be strictly after estimated arrival time.");
        }
    }

    private void validateConflicts(Long vesselId, Long berthId, LocalDateTime eta, LocalDateTime etd, Long excludeId) {
        LocalDateTime effectiveEnd = etd != null ? etd : eta.plusHours(12);

        // 1. Vessel schedule overlap check
        List<ShippingSchedule> vesselConflicts = scheduleRepository.findVesselScheduleConflicts(vesselId, eta, effectiveEnd, excludeId);
        if (!vesselConflicts.isEmpty()) {
            ShippingSchedule c = vesselConflicts.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Schedule conflict: Vessel is already scheduled for port call #" + c.getId() + " during " + eta + " to " + effectiveEnd);
        }

        // 2. Berth schedule & allocation overlap check
        if (berthId != null) {
            List<ShippingSchedule> berthConflicts = scheduleRepository.findBerthScheduleConflicts(berthId, eta, effectiveEnd, excludeId);
            if (!berthConflicts.isEmpty()) {
                ShippingSchedule c = berthConflicts.get(0);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Schedule conflict: Berth is already reserved by schedule #" + c.getId() + " during requested time window.");
            }

            // Check berth_allocations table overlap
            Instant startInstant = eta.atZone(ZoneId.systemDefault()).toInstant();
            Instant endInstant = effectiveEnd.atZone(ZoneId.systemDefault()).toInstant();
            boolean hasAllocationConflict = allocationRepository.hasConflictExcluding(berthId, null, startInstant, endInstant, null);
            if (hasAllocationConflict) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Schedule conflict: Berth #" + berthId + " already has an active berth allocation during the requested window.");
            }
        }
    }

    private void recordAuditLog(User user, String action, Long entityId, Map<String, Object> detailsMap) {
        try {
            String detailsJson = objectMapper.writeValueAsString(detailsMap);
            AuditLog log = new AuditLog(user, action, "ShippingSchedule", entityId, detailsJson);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Non-blocking fallback
        }
    }
}
