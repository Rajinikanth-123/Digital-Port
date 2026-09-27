package com.portshipping.service;

import com.portshipping.entity.Port;
import com.portshipping.entity.Terminal;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.BerthRepository;
import com.portshipping.repository.PortRepository;
import com.portshipping.repository.TerminalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TerminalService {

    private final TerminalRepository terminalRepository;
    private final PortRepository portRepository;
    private final BerthRepository berthRepository;

    public TerminalService(TerminalRepository terminalRepository, PortRepository portRepository, BerthRepository berthRepository) {
        this.terminalRepository = terminalRepository;
        this.portRepository = portRepository;
        this.berthRepository = berthRepository;
    }

    public Page<Terminal> getTerminals(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String q = search.trim();
            return terminalRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(q, q, pageable);
        }
        return terminalRepository.findAll(pageable);
    }

    public Terminal getTerminal(Long id) {
        return terminalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Terminal not found with ID: " + id));
    }

    @Transactional
    public Terminal createTerminal(Long portId, String name, String code, String terminalType, Integer capacity) {
        Port port = portRepository.findById(portId)
                .orElseThrow(() -> new NotFoundException("Parent Port not found with ID: " + portId));

        String cleanCode = code.trim().toUpperCase();
        if (terminalRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new ConflictException("Terminal code '" + cleanCode + "' is already in use by another terminal.");
        }

        if (capacity != null && capacity < 0) {
            throw new IllegalArgumentException("Terminal capacity cannot be negative.");
        }

        Terminal terminal = new Terminal(port, name.trim(), cleanCode, terminalType, capacity);
        return terminalRepository.save(terminal);
    }

    @Transactional
    public Terminal updateTerminal(Long id, Long portId, String name, String code, String terminalType, Integer capacity) {
        Terminal terminal = getTerminal(id);
        Port port = portRepository.findById(portId)
                .orElseThrow(() -> new NotFoundException("Parent Port not found with ID: " + portId));

        String cleanCode = code.trim().toUpperCase();
        if (terminalRepository.existsByCodeIgnoreCaseAndIdNot(cleanCode, id)) {
            throw new ConflictException("Terminal code '" + cleanCode + "' is already in use by another terminal.");
        }

        if (capacity != null && capacity < 0) {
            throw new IllegalArgumentException("Terminal capacity cannot be negative.");
        }

        terminal.setPort(port);
        terminal.setName(name.trim());
        terminal.setCode(cleanCode);
        if (terminalType != null && !terminalType.isBlank()) {
            terminal.setTerminalType(terminalType.trim());
        }
        terminal.setCapacity(capacity != null ? capacity : 0);

        return terminalRepository.save(terminal);
    }

    @Transactional
    public void deleteTerminal(Long id) {
        Terminal terminal = getTerminal(id);
        long berthCount = berthRepository.countByTerminalId(id);
        if (berthCount > 0) {
            throw new ConflictException("Cannot delete Terminal '" + terminal.getName() + "': It is referenced by " + berthCount + " berth(s).");
        }
        terminalRepository.delete(terminal);
    }
}
