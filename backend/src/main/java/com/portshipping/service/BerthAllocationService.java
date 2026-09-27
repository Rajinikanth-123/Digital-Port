package com.portshipping.service;

import com.portshipping.entity.Berth;
import com.portshipping.entity.BerthAllocation;
import com.portshipping.entity.Vessel;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.BerthAllocationRepository;
import com.portshipping.repository.BerthRepository;
import com.portshipping.repository.VesselRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BerthAllocationService {

    private final BerthAllocationRepository allocations;
    private final VesselRepository vessels;
    private final BerthRepository berths;

    public BerthAllocationService(BerthAllocationRepository allocations, VesselRepository vessels, BerthRepository berths) {
        this.allocations = allocations;
        this.vessels = vessels;
        this.berths = berths;
    }

    public List<BerthAllocation> all() {
        return allocations.findAll();
    }

    public Page<BerthAllocation> page(Pageable pageable) {
        return allocations.findAll(pageable);
    }

    public BerthAllocation get(Long id) {
        return allocations.findById(id)
                .orElseThrow(() -> new NotFoundException("Berth allocation not found with ID: " + id));
    }

    @Transactional
    public BerthAllocation create(Long berthId, String berthName, Instant start, Instant end, Long vesselId) {
        validateTimeRange(start, end);

        Vessel vessel = vessels.findById(vesselId)
                .orElseThrow(() -> new NotFoundException("Vessel not found with ID: " + vesselId));

        Berth berthEntity = null;
        if (berthId != null) {
            berthEntity = berths.findById(berthId)
                    .orElseThrow(() -> new NotFoundException("Berth not found with ID: " + berthId));
            if (berthName == null || berthName.isBlank()) {
                berthName = berthEntity.getName();
            }
        }

        if (berthName == null || berthName.isBlank()) {
            throw new IllegalArgumentException("Berth name or berth ID is required.");
        }

        if (allocations.hasConflict(berthId, berthName, start, end)) {
            throw new ConflictException("Berth allocation conflict: Berth '" + berthName + "' is already allocated between " + start + " and " + end + ".");
        }

        BerthAllocation allocation = new BerthAllocation(berthEntity, berthName, start, end, vessel, "SCHEDULED");
        return allocations.save(allocation);
    }

    @Transactional
    public BerthAllocation create(String berthName, Instant start, Instant end, Long vesselId) {
        return create(null, berthName, start, end, vesselId);
    }

    @Transactional
    public BerthAllocation update(Long id, Long berthId, String berthName, Instant start, Instant end, Long vesselId, String status) {
        BerthAllocation allocation = get(id);
        validateTimeRange(start, end);

        Vessel vessel = vessels.findById(vesselId)
                .orElseThrow(() -> new NotFoundException("Vessel not found with ID: " + vesselId));

        Berth berthEntity = null;
        if (berthId != null) {
            berthEntity = berths.findById(berthId)
                    .orElseThrow(() -> new NotFoundException("Berth not found with ID: " + berthId));
            if (berthName == null || berthName.isBlank()) {
                berthName = berthEntity.getName();
            }
        }

        if (berthName == null || berthName.isBlank()) {
            berthName = allocation.getBerth();
        }

        if (allocations.hasConflictExcluding(berthId, berthName, start, end, id)) {
            throw new ConflictException("Berth allocation conflict: Berth '" + berthName + "' is already allocated between " + start + " and " + end + ".");
        }

        allocation.setBerthEntity(berthEntity);
        allocation.setBerth(berthName);
        allocation.setStartTime(start);
        allocation.setEndTime(end);
        allocation.setVessel(vessel);
        if (status != null && !status.isBlank()) {
            allocation.setStatus(status.trim());
        }

        return allocations.save(allocation);
    }

    @Transactional
    public void delete(Long id) {
        BerthAllocation allocation = get(id);
        allocations.delete(allocation);
    }

    private void validateTimeRange(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time are required.");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Arrival/Start time (" + start + ") must be before Departure/End time (" + end + ").");
        }
    }
}
