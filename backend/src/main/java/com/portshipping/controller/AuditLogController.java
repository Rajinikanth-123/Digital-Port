package com.portshipping.controller;

import com.portshipping.entity.AuditLog;
import com.portshipping.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "action", "entityName", "outcome", "id");

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    public record AuditLogDTO(
            Long id,
            Long userId,
            String actorName,
            String actorRole,
            String action,
            String entityName,
            Long entityId,
            String outcome,
            String ipAddress,
            String description,
            String details,
            Instant createdAt
    ) {}

    public record AuditLogSummaryDTO(
            long totalEvents,
            long successCount,
            long failureCount,
            long activeActorsCount
    ) {}

    private AuditLogDTO toDTO(AuditLog a) {
        Long userId = a.getUser() != null ? a.getUser().getId() : null;
        String actorName = a.getActorName() != null ? a.getActorName() : (a.getUser() != null ? a.getUser().getName() : "System");
        String actorRole = a.getActorRole() != null ? a.getActorRole() : (a.getUser() != null ? a.getUser().getRole() : "SYSTEM");

        return new AuditLogDTO(
                a.getId(),
                userId,
                actorName,
                actorRole,
                a.getAction(),
                a.getEntityName(),
                a.getEntityId(),
                a.getOutcome(),
                a.getIpAddress(),
                a.getDescription(),
                a.getDetails(),
                a.getCreatedAt()
        );
    }

    @GetMapping
    public Page<AuditLogDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityName,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String outcome
    ) {
        String cleanSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(direction, cleanSortBy));

        Instant startInstant = parseDateOrInstant(startDate, true);
        Instant endInstant = parseDateOrInstant(endDate, false);

        if (startInstant != null && endInstant != null && startInstant.isAfter(endInstant)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate cannot be after endDate");
        }

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startInstant));
            }
            if (endInstant != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endInstant));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), actorId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase()));
            }
            if (entityName != null && !entityName.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("entityName")), entityName.trim().toLowerCase()));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (outcome != null && !outcome.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("outcome")), outcome.trim().toUpperCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> pageResult = repo.findAll(spec, pageRequest);
        List<AuditLogDTO> dtos = pageResult.getContent().stream().map(this::toDTO).toList();
        return new PageImpl<>(dtos, pageResult.getPageable(), pageResult.getTotalElements());
    }

    @GetMapping("/summary")
    public AuditLogSummaryDTO getSummary() {
        long total = repo.count();
        long success = repo.countByOutcomeIgnoreCase("SUCCESS");
        long failure = repo.countByOutcomeIgnoreCase("FAILURE");
        long activeActors = repo.countDistinctActiveUsers();
        return new AuditLogSummaryDTO(total, success, failure, activeActors);
    }

    @GetMapping("/{id}")
    public AuditLogDTO getById(@PathVariable Long id) {
        AuditLog auditLog = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit Log " + id + " not found"));
        return toDTO(auditLog);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public List<AuditLogDTO> getByEntity(@PathVariable String entityType, @PathVariable Long entityId) {
        return repo.findByEntityNameIgnoreCaseAndEntityIdOrderByCreatedAtDesc(entityType, entityId)
                .stream().map(this::toDTO).toList();
    }

    @GetMapping("/recent")
    public List<AuditLogDTO> recent() {
        return repo.findTop20ByOrderByCreatedAtDesc().stream().map(this::toDTO).toList();
    }

    private Instant parseDateOrInstant(String input, boolean startOfDay) {
        if (input == null || input.isBlank()) return null;
        try {
            return Instant.parse(input);
        } catch (Exception e) {
            try {
                LocalDate date = LocalDate.parse(input);
                return startOfDay ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format: " + input + ". Use ISO-8601 (yyyy-MM-dd or yyyy-MM-ddTHH:mm:ssZ)");
            }
        }
    }
}
