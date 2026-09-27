package com.portshipping.service;

import com.portshipping.entity.Berth;
import com.portshipping.entity.Terminal;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.BerthAllocationRepository;
import com.portshipping.repository.BerthRepository;
import com.portshipping.repository.ShippingScheduleRepository;
import com.portshipping.repository.TerminalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BerthService {

    private final BerthRepository berthRepository;
    private final TerminalRepository terminalRepository;
    private final BerthAllocationRepository allocationRepository;
    private final ShippingScheduleRepository scheduleRepository;

    public BerthService(BerthRepository berthRepository,
                        TerminalRepository terminalRepository,
                        BerthAllocationRepository allocationRepository,
                        ShippingScheduleRepository scheduleRepository) {
        this.berthRepository = berthRepository;
        this.terminalRepository = terminalRepository;
        this.allocationRepository = allocationRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Page<Berth> getBerths(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return berthRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        }
        return berthRepository.findAll(pageable);
    }

    public Berth getBerth(Long id) {
        return berthRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Berth not found with ID: " + id));
    }

    @Transactional
    public Berth createBerth(Long terminalId, String name, Double maxLengthM, String status) {
        Terminal terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new NotFoundException("Parent Terminal not found with ID: " + terminalId));

        String cleanName = name.trim();
        if (berthRepository.existsByTerminalIdAndNameIgnoreCase(terminalId, cleanName)) {
            throw new ConflictException("Berth '" + cleanName + "' already exists in Terminal '" + terminal.getName() + "'.");
        }

        if (maxLengthM != null && maxLengthM <= 0) {
            throw new IllegalArgumentException("Max length must be a positive number.");
        }

        Berth berth = new Berth(terminal, cleanName, maxLengthM != null ? maxLengthM : 300.0, status);
        return berthRepository.save(berth);
    }

    @Transactional
    public Berth updateBerth(Long id, Long terminalId, String name, Double maxLengthM, String status) {
        Berth berth = getBerth(id);
        Terminal terminal = terminalRepository.findById(terminalId)
                .orElseThrow(() -> new NotFoundException("Parent Terminal not found with ID: " + terminalId));

        String cleanName = name.trim();
        if (berthRepository.existsByTerminalIdAndNameIgnoreCaseAndIdNot(terminalId, cleanName, id)) {
            throw new ConflictException("Berth '" + cleanName + "' already exists in Terminal '" + terminal.getName() + "'.");
        }

        if (maxLengthM != null && maxLengthM <= 0) {
            throw new IllegalArgumentException("Max length must be a positive number.");
        }

        berth.setTerminal(terminal);
        berth.setName(cleanName);
        if (maxLengthM != null) berth.setMaxLengthM(maxLengthM);
        if (status != null && !status.isBlank()) berth.setStatus(status.trim());

        return berthRepository.save(berth);
    }

    @Transactional
    public void deleteBerth(Long id) {
        Berth berth = getBerth(id);

        long allocations = allocationRepository.countByBerthEntityId(id);
        long schedules = scheduleRepository.countByBerthId(id);

        if (allocations > 0 || schedules > 0) {
            throw new ConflictException("Cannot delete Berth '" + berth.getName() + "': Referenced by " +
                    allocations + " active allocation(s) and " + schedules + " schedule(s).");
        }

        berthRepository.delete(berth);
    }
}
