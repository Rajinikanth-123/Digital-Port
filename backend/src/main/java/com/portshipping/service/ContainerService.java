package com.portshipping.service;

import com.portshipping.entity.Container;
import com.portshipping.entity.Shipment;
import com.portshipping.repository.ContainerRepository;
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
public class ContainerService {

    private final ContainerRepository containerRepository;
    private final ShipmentRepository shipmentRepository;

    public ContainerService(ContainerRepository containerRepository, ShipmentRepository shipmentRepository) {
        this.containerRepository = containerRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Container> getAllContainers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (search != null && !search.trim().isEmpty()) {
            return containerRepository.searchContainers(search.trim(), pageable);
        }
        return containerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Container> getAllContainersList() {
        return containerRepository.findAll(Sort.by("id").descending());
    }

    @Transactional(readOnly = true)
    public Container getContainerById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found with ID: " + id));
    }

    public Container createContainer(String containerNumber, String size, String type, BigDecimal weightKg, String location, String status, Long shipmentId) {
        if (containerNumber == null || containerNumber.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container number is required.");
        }

        String formattedNumber = containerNumber.trim().toUpperCase();
        if (containerRepository.existsByContainerNumberIgnoreCase(formattedNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Container number '" + formattedNumber + "' already exists.");
        }

        if (weightKg != null && weightKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container weight cannot be negative.");
        }

        Shipment shipment = null;
        if (shipmentId != null) {
            shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipment ID: " + shipmentId));
        }

        Container container = new Container(formattedNumber, size != null ? size : "40FT", type != null ? type : "DRY", weightKg, location, status != null ? status : "AVAILABLE", shipment);
        return containerRepository.save(container);
    }

    public Container updateContainer(Long id, String containerNumber, String size, String type, BigDecimal weightKg, String location, String status, Long shipmentId) {
        Container container = getContainerById(id);

        if (containerNumber != null && !containerNumber.trim().isEmpty()) {
            String formattedNumber = containerNumber.trim().toUpperCase();
            if (containerRepository.existsByContainerNumberIgnoreCaseAndIdNot(formattedNumber, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Container number '" + formattedNumber + "' already exists.");
            }
            container.setContainerNumber(formattedNumber);
        }

        if (size != null && !size.trim().isEmpty()) {
            container.setSize(size.trim());
        }

        if (type != null && !type.trim().isEmpty()) {
            container.setType(type.trim());
        }

        if (weightKg != null) {
            if (weightKg.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container weight cannot be negative.");
            }
            container.setWeightKg(weightKg);
        }

        if (location != null) {
            container.setLocation(location.trim());
        }

        if (status != null && !status.trim().isEmpty()) {
            container.setStatus(status.trim().toUpperCase());
        }

        if (shipmentId != null) {
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipment ID: " + shipmentId));
            container.setShipment(shipment);
        } else {
            container.setShipment(null);
        }

        return containerRepository.save(container);
    }

    public void deleteContainer(Long id) {
        Container container = getContainerById(id);
        containerRepository.delete(container);
    }
}
