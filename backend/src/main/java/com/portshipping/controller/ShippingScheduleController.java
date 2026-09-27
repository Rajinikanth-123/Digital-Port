package com.portshipping.controller;

import com.portshipping.entity.ShippingSchedule;
import com.portshipping.entity.User;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.ShippingScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/schedules")
public class ShippingScheduleController {

    private final ShippingScheduleService scheduleService;
    private final UserRepository userRepository;

    public ShippingScheduleController(ShippingScheduleService scheduleService, UserRepository userRepository) {
        this.scheduleService = scheduleService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<ShippingSchedule>> getSchedules(@RequestParam(required = false) String search,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long vesselId,
                                                               @RequestParam(required = false) Long berthId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size) {
        Page<ShippingSchedule> result = scheduleService.getSchedules(search, status, vesselId, berthId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "arrivalEstimated")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShippingSchedule> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @PostMapping
    public ResponseEntity<ShippingSchedule> createSchedule(@Valid @RequestBody ScheduleRequest req, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        ShippingSchedule created = scheduleService.createSchedule(
                req.getVesselId(),
                req.getBerthId(),
                req.getArrivalEstimated(),
                req.getDepartureEstimated(),
                req.getStatus(),
                currentUser
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingSchedule> updateSchedule(@PathVariable Long id,
                                                           @Valid @RequestBody ScheduleRequest req,
                                                           Authentication authentication) {
        User currentUser = resolveUser(authentication);
        ShippingSchedule updated = scheduleService.updateSchedule(
                id,
                req.getVesselId(),
                req.getBerthId(),
                req.getArrivalEstimated(),
                req.getDepartureEstimated(),
                req.getStatus(),
                currentUser
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        scheduleService.deleteSchedule(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    public static class ScheduleRequest {

        private Long vesselId;
        private Long berthId;
        private VesselNested vessel;
        private BerthNested berth;

        @NotNull(message = "Estimated arrival time is required")
        private LocalDateTime arrivalEstimated;

        private LocalDateTime departureEstimated;
        private String status = "SCHEDULED";

        public Long getVesselId() {
            if (vesselId != null) return vesselId;
            return vessel != null ? vessel.getId() : null;
        }

        public void setVesselId(Long vesselId) { this.vesselId = vesselId; }

        public Long getBerthId() {
            if (berthId != null) return berthId;
            return berth != null ? berth.getId() : null;
        }

        public void setBerthId(Long berthId) { this.berthId = berthId; }

        public VesselNested getVessel() { return vessel; }
        public void setVessel(VesselNested vessel) { this.vessel = vessel; }

        public BerthNested getBerth() { return berth; }
        public void setBerth(BerthNested berth) { this.berth = berth; }

        public LocalDateTime getArrivalEstimated() { return arrivalEstimated; }
        public void setArrivalEstimated(LocalDateTime arrivalEstimated) { this.arrivalEstimated = arrivalEstimated; }

        public LocalDateTime getDepartureEstimated() { return departureEstimated; }
        public void setDepartureEstimated(LocalDateTime departureEstimated) { this.departureEstimated = departureEstimated; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public static class VesselNested {
            private Long id;
            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
        }

        public static class BerthNested {
            private Long id;
            public Long getId() { return id; }
            public void setId(Long id) { this.id = id; }
        }
    }
}
