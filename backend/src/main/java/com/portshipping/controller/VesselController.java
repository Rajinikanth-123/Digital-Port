package com.portshipping.controller;

import com.portshipping.entity.AuditLog;
import com.portshipping.entity.User;
import com.portshipping.entity.Vessel;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.VesselService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vessels")
public class VesselController {

    private final VesselService vesselService;
    private final UserRepository userRepository;

    public VesselController(VesselService vesselService, UserRepository userRepository) {
        this.vesselService = vesselService;
        this.userRepository = userRepository;
    }

    public record VesselRequest(
            @NotBlank(message = "Vessel name is required") @Size(max = 160) String name,
            @NotBlank(message = "IMO number is required") @Size(max = 20) String imoNumber,
            String vesselType,
            String flag,
            @Min(value = 0, message = "Capacity must be zero or positive") Integer capacity,
            String operatorName,
            @Positive(message = "Length must be a positive number") Double length,
            @Positive(message = "Draft must be a positive number") Double draft,
            String status
    ) {}

    public record TrackingUpdateRequest(
            @Min(value = -90, message = "Latitude must be >= -90") @Max(value = 90, message = "Latitude must be <= 90") Double latitude,
            @Min(value = -180, message = "Longitude must be >= -180") @Max(value = 180, message = "Longitude must be <= 180") Double longitude,
            String currentLocation,
            String status
    ) {}

    @GetMapping
    public Page<Vessel> all(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return vesselService.getVessels(search, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public Vessel one(@PathVariable Long id) {
        return vesselService.getVessel(id);
    }

    @PostMapping
    public ResponseEntity<Vessel> create(@Valid @RequestBody VesselRequest r) {
        Vessel created = vesselService.createVessel(
                r.name(), r.imoNumber(), r.vesselType(), r.flag(), r.capacity(),
                r.operatorName(), r.length(), r.draft(), r.status()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Vessel update(@PathVariable Long id, @Valid @RequestBody VesselRequest r) {
        return vesselService.updateVessel(
                id, r.name(), r.imoNumber(), r.vesselType(), r.flag(), r.capacity(),
                r.operatorName(), r.length(), r.draft(), r.status()
        );
    }

    @PostMapping("/{id}/tracking")
    public ResponseEntity<Vessel> updateTracking(@PathVariable Long id,
                                                 @Valid @RequestBody TrackingUpdateRequest r,
                                                 Authentication authentication) {
        User currentUser = resolveUser(authentication);
        Vessel updated = vesselService.updateVesselTracking(id, r.latitude(), r.longitude(), r.currentLocation(), r.status(), currentUser);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<List<AuditLog>> getTrackingHistory(@PathVariable Long id) {
        List<AuditLog> history = vesselService.getVesselTrackingHistory(id);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vesselService.deleteVessel(id);
        return ResponseEntity.noContent().build();
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }
}
