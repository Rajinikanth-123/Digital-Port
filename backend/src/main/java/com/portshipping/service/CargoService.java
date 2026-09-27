package com.portshipping.service;

import com.portshipping.entity.Cargo;
import com.portshipping.entity.Shipment;
import com.portshipping.repository.CargoRepository;
import com.portshipping.repository.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CargoService {

    private final CargoRepository cargoRepository;
    private final ShipmentRepository shipmentRepository;

    public CargoService(CargoRepository cargoRepository, ShipmentRepository shipmentRepository) {
        this.cargoRepository = cargoRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Cargo> getAllCargo(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (search != null && !search.trim().isEmpty()) {
            return cargoRepository.searchCargo(search.trim(), pageable);
        }
        return cargoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Cargo> getAllCargoList() {
        return cargoRepository.findAll(Sort.by("id").descending());
    }

    @Transactional(readOnly = true)
    public Cargo getCargoById(Long id) {
        return cargoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cargo item not found with ID: " + id));
    }

    public Cargo createCargo(Long shipmentId, String description, String cargoType, Integer quantity, BigDecimal weightKg) {
        if (shipmentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent shipment ID is required.");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipment ID: " + shipmentId));

        if (description == null || description.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo description is required.");
        }

        if (quantity != null && quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo quantity must be at least 1.");
        }

        if (weightKg != null && weightKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo weight cannot be negative.");
        }

        Cargo cargo = new Cargo(shipment, description.trim(), cargoType != null ? cargoType.trim() : "GENERAL", quantity != null ? quantity : 1, weightKg != null ? weightKg : BigDecimal.ZERO);
        return cargoRepository.save(cargo);
    }

    public Cargo updateCargo(Long id, Long shipmentId, String description, String cargoType, Integer quantity, BigDecimal weightKg) {
        Cargo cargo = getCargoById(id);

        if (shipmentId != null) {
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipment ID: " + shipmentId));
            cargo.setShipment(shipment);
        }

        if (description != null && !description.trim().isEmpty()) {
            cargo.setDescription(description.trim());
        }

        if (cargoType != null && !cargoType.trim().isEmpty()) {
            cargo.setCargoType(cargoType.trim());
        }

        if (quantity != null) {
            if (quantity < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo quantity must be at least 1.");
            }
            cargo.setQuantity(quantity);
        }

        if (weightKg != null) {
            if (weightKg.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cargo weight cannot be negative.");
            }
            cargo.setWeightKg(weightKg);
        }

        return cargoRepository.save(cargo);
    }

    public void deleteCargo(Long id) {
        Cargo cargo = getCargoById(id);
        cargoRepository.delete(cargo);
    }
}
