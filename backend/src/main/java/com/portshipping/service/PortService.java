package com.portshipping.service;

import com.portshipping.entity.Port;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.PortRepository;
import com.portshipping.repository.TerminalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortService {

    private final PortRepository portRepository;
    private final TerminalRepository terminalRepository;

    public PortService(PortRepository portRepository, TerminalRepository terminalRepository) {
        this.portRepository = portRepository;
        this.terminalRepository = terminalRepository;
    }

    public Page<Port> getPorts(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String q = search.trim();
            return portRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrCountryContainingIgnoreCase(q, q, q, pageable);
        }
        return portRepository.findAll(pageable);
    }

    public Port getPort(Long id) {
        return portRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Port not found with ID: " + id));
    }

    @Transactional
    public Port createPort(String name, String code, String country, String timezone, Double latitude, Double longitude) {
        String cleanCode = code.trim().toUpperCase();
        if (portRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ConflictException("Port code '" + cleanCode + "' is already in use by another port.");
        }
        Port port = new Port(name.trim(), cleanCode, country.trim(), timezone, latitude, longitude);
        return portRepository.save(port);
    }

    @Transactional
    public Port updatePort(Long id, String name, String code, String country, String timezone, Double latitude, Double longitude) {
        Port port = getPort(id);
        String cleanCode = code.trim().toUpperCase();
        if (portRepository.existsByCodeIgnoreCaseAndIdNot(cleanCode, id)) {
            throw new ConflictException("Port code '" + cleanCode + "' is already in use by another port.");
        }

        port.setName(name.trim());
        port.setCode(cleanCode);
        port.setCountry(country.trim());
        if (timezone != null && !timezone.isBlank()) {
            port.setTimezone(timezone.trim());
        }
        port.setLatitude(latitude);
        port.setLongitude(longitude);

        return portRepository.save(port);
    }

    @Transactional
    public void deletePort(Long id) {
        Port port = getPort(id);
        long terminalCount = terminalRepository.countByPortId(id);
        if (terminalCount > 0) {
            throw new ConflictException("Cannot delete Port '" + port.getName() + "': It is referenced by " + terminalCount + " terminal(s).");
        }
        portRepository.delete(port);
    }
}
