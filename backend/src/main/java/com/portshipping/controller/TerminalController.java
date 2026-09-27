package com.portshipping.controller;

import com.portshipping.entity.Terminal;
import com.portshipping.service.TerminalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terminals")
public class TerminalController {

    private final TerminalService terminalService;

    public TerminalController(TerminalService terminalService) {
        this.terminalService = terminalService;
    }

    public record TerminalRequest(
            @NotBlank(message = "Terminal name is required") @Size(max = 120) String name,
            @NotBlank(message = "Terminal code is required") @Size(max = 20) String code,
            Long portId,
            PortRef port,
            String terminalType,
            @Min(value = 0, message = "Capacity must be zero or positive") Integer capacity
    ) {
        public Long getResolvedPortId() {
            if (portId != null) return portId;
            if (port != null && port.id() != null) return port.id();
            return null;
        }
    }

    public record PortRef(@NotNull Long id) {}

    @GetMapping
    public Page<Terminal> all(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        return terminalService.getTerminals(search, PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public Terminal one(@PathVariable Long id) {
        return terminalService.getTerminal(id);
    }

    @PostMapping
    public ResponseEntity<Terminal> create(@Valid @RequestBody TerminalRequest req) {
        Long pId = req.getResolvedPortId();
        if (pId == null) {
            throw new IllegalArgumentException("Parent port ID is required.");
        }
        Terminal created = terminalService.createTerminal(
                pId, req.name(), req.code(), req.terminalType(), req.capacity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Terminal update(@PathVariable Long id, @Valid @RequestBody TerminalRequest req) {
        Long pId = req.getResolvedPortId();
        if (pId == null) {
            throw new IllegalArgumentException("Parent port ID is required.");
        }
        return terminalService.updateTerminal(
                id, pId, req.name(), req.code(), req.terminalType(), req.capacity()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        terminalService.deleteTerminal(id);
        return ResponseEntity.noContent().build();
    }
}
