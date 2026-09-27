package com.portshipping.controller;

import com.portshipping.entity.BerthAllocation;
import com.portshipping.service.BerthAllocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/berth-allocations")
public class BerthAllocationController {

    private final BerthAllocationService service;

    public BerthAllocationController(BerthAllocationService s) {
        this.service = s;
    }

    public record Request(
            Long berthId,
            String berth,
            @NotNull(message = "Start time is required") Instant startTime,
            @NotNull(message = "End time is required") Instant endTime,
            @NotNull(message = "Target vessel ID is required") Long vesselId,
            String status
    ) {}

    @GetMapping
    public Object all(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null && size != null) {
            return service.page(PageRequest.of(page, size, Sort.by("startTime").descending()));
        }
        return service.all();
    }

    @GetMapping("/{id}")
    public BerthAllocation one(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<BerthAllocation> create(@Valid @RequestBody Request r) {
        BerthAllocation created = service.create(
                r.berthId(), r.berth(), r.startTime(), r.endTime(), r.vesselId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public BerthAllocation update(@PathVariable Long id, @Valid @RequestBody Request r) {
        return service.update(
                id, r.berthId(), r.berth(), r.startTime(), r.endTime(), r.vesselId(), r.status()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
