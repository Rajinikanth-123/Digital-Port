package com.portshipping.controller;

import com.portshipping.entity.Cargo;
import com.portshipping.service.CargoService;
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
@RequestMapping("/api/cargo")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @GetMapping
    public ResponseEntity<?> getCargo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        if (size >= 100) {
            List<Cargo> list = cargoService.getAllCargoList();
            return ResponseEntity.ok(list);
        }
        Page<Cargo> paged = cargoService.getAllCargo(page, size, search);
        return ResponseEntity.ok(paged);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cargo> getCargoById(@PathVariable Long id) {
        return ResponseEntity.ok(cargoService.getCargoById(id));
    }

    @PostMapping
    public ResponseEntity<Cargo> createCargo(@Valid @RequestBody CargoRequest req) {
        Long shipmentId = req.getShipmentId();
        if (shipmentId == null && req.getShipment() != null) {
            shipmentId = req.getShipment().getId();
        }

        Cargo created = cargoService.createCargo(
                shipmentId,
                req.getDescription(),
                req.getCargoType(),
                req.getQuantity(),
                req.getWeightKg()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cargo> updateCargo(@PathVariable Long id, @Valid @RequestBody CargoRequest req) {
        Long shipmentId = req.getShipmentId();
        if (shipmentId == null && req.getShipment() != null) {
            shipmentId = req.getShipment().getId();
        }

        Cargo updated = cargoService.updateCargo(
                id,
                shipmentId,
                req.getDescription(),
                req.getCargoType(),
                req.getQuantity(),
                req.getWeightKg()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCargo(@PathVariable Long id) {
        cargoService.deleteCargo(id);
        return ResponseEntity.noContent().build();
    }

    public static class ShipmentRef {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class CargoRequest {
        @NotBlank(message = "Cargo description is required")
        private String description;
        private String cargoType;
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
        @Min(value = 0, message = "Weight cannot be negative")
        private BigDecimal weightKg;
        private Long shipmentId;
        private ShipmentRef shipment;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCargoType() { return cargoType; }
        public void setCargoType(String cargoType) { this.cargoType = cargoType; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

        public Long getShipmentId() { return shipmentId; }
        public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

        public ShipmentRef getShipment() { return shipment; }
        public void setShipment(ShipmentRef shipment) { this.shipment = shipment; }
    }
}
