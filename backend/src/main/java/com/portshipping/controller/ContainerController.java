package com.portshipping.controller;

import com.portshipping.entity.Container;
import com.portshipping.service.ContainerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService) {
        this.containerService = containerService;
    }

    @GetMapping
    public ResponseEntity<?> getContainers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        if (size >= 100) {
            List<Container> list = containerService.getAllContainersList();
            return ResponseEntity.ok(list);
        }
        Page<Container> paged = containerService.getAllContainers(page, size, search);
        return ResponseEntity.ok(paged);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Container> getContainerById(@PathVariable Long id) {
        return ResponseEntity.ok(containerService.getContainerById(id));
    }

    @PostMapping
    public ResponseEntity<Container> createContainer(@Valid @RequestBody ContainerRequest req) {
        Container created = containerService.createContainer(
                req.getContainerNumber(),
                req.getSize(),
                req.getType(),
                req.getWeightKg(),
                req.getLocation(),
                req.getStatus(),
                req.getShipmentId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Container> updateContainer(@PathVariable Long id, @Valid @RequestBody ContainerRequest req) {
        Container updated = containerService.updateContainer(
                id,
                req.getContainerNumber(),
                req.getSize(),
                req.getType(),
                req.getWeightKg(),
                req.getLocation(),
                req.getStatus(),
                req.getShipmentId()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContainer(@PathVariable Long id) {
        containerService.deleteContainer(id);
        return ResponseEntity.noContent().build();
    }

    public static class ContainerRequest {
        @NotBlank(message = "Container number is required")
        private String containerNumber;
        private String size;
        private String type;
        @Min(value = 0, message = "Weight cannot be negative")
        private BigDecimal weightKg;
        private String location;
        private String status;
        private Long shipmentId;

        public String getContainerNumber() { return containerNumber; }
        public void setContainerNumber(String containerNumber) { this.containerNumber = containerNumber; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    }
}
