package com.portshipping.controller;

import com.portshipping.entity.Berth;
import com.portshipping.service.BerthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/berths")
public class BerthController {

    private final BerthService berthService;

    public BerthController(BerthService berthService) {
        this.berthService = berthService;
    }

    public record BerthRequest(
            @NotBlank(message = "Berth name is required") @Size(max = 80) String name,
            Long terminalId,
            TerminalRef terminal,
            @Positive(message = "Max length must be a positive number") Double maxLengthM,
            String status
    ) {
        public Long getResolvedTerminalId() {
            if (terminalId != null) return terminalId;
            if (terminal != null && terminal.id() != null) return terminal.id();
            return null;
        }
    }

    public record TerminalRef(@NotNull Long id) {}

    @GetMapping
    public Page<Berth> all(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return berthService.getBerths(search, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public Berth one(@PathVariable Long id) {
        return berthService.getBerth(id);
    }

    @PostMapping
    public ResponseEntity<Berth> create(@Valid @RequestBody BerthRequest req) {
        Long tId = req.getResolvedTerminalId();
        if (tId == null) {
            throw new IllegalArgumentException("Parent terminal ID is required.");
        }
        Berth created = berthService.createBerth(tId, req.name(), req.maxLengthM(), req.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Berth update(@PathVariable Long id, @Valid @RequestBody BerthRequest req) {
        Long tId = req.getResolvedTerminalId();
        if (tId == null) {
            throw new IllegalArgumentException("Parent terminal ID is required.");
        }
        return berthService.updateBerth(id, tId, req.name(), req.maxLengthM(), req.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        berthService.deleteBerth(id);
        return ResponseEntity.noContent().build();
    }
}
