package com.portshipping.controller;

import com.portshipping.entity.Port;
import com.portshipping.service.PortService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ports")
public class PortController {

    private final PortService portService;

    public PortController(PortService portService) {
        this.portService = portService;
    }

    public record PortRequest(
            @NotBlank(message = "Port name is required") @Size(max = 120) String name,
            @NotBlank(message = "Port code is required") @Size(max = 10) String code,
            @NotBlank(message = "Country is required") @Size(max = 80) String country,
            String timezone,
            Double latitude,
            Double longitude
    ) {}

    @GetMapping
    public Page<Port> all(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return portService.getPorts(search, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public Port one(@PathVariable Long id) {
        return portService.getPort(id);
    }

    @PostMapping
    public ResponseEntity<Port> create(@Valid @RequestBody PortRequest req) {
        Port created = portService.createPort(
                req.name(), req.code(), req.country(), req.timezone(), req.latitude(), req.longitude()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Port update(@PathVariable Long id, @Valid @RequestBody PortRequest req) {
        return portService.updatePort(
                id, req.name(), req.code(), req.country(), req.timezone(), req.latitude(), req.longitude()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        portService.deletePort(id);
        return ResponseEntity.noContent().build();
    }
}
