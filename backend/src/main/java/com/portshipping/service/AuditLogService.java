package com.portshipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.entity.AuditLog;
import com.portshipping.entity.User;
import com.portshipping.repository.AuditLogRepository;
import com.portshipping.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "passwordhash", "password_hash", "secret", "token", "accesstoken",
            "refreshtoken", "apikey", "creditcard", "cardnumber", "cvv", "authorization"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public User getCurrentAuthenticatedUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            log.trace("Unable to resolve authenticated user from context: {}", e.getMessage());
        }
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logEvent(User actor, String action, String entityName, Long entityId, String outcome, String ipAddress, String description, String detailsJson) {
        try {
            User currentActor = actor != null ? actor : getCurrentAuthenticatedUser();
            String actorName = currentActor != null ? currentActor.getName() : "System";
            String actorRole = currentActor != null ? currentActor.getRole() : "SYSTEM";
            String sanitizedOutcome = (outcome != null && !outcome.isBlank()) ? outcome.toUpperCase() : "SUCCESS";

            User managedActor = null;
            if (currentActor != null && currentActor.getId() != null) {
                managedActor = userRepository.findById(currentActor.getId()).orElse(null);
            }

            String sanitizedDesc = sanitizeText(description);
            String sanitizedDetails = redactSensitiveJson(detailsJson);

            AuditLog auditLog = new AuditLog(
                    managedActor,
                    actorName,
                    actorRole,
                    action,
                    entityName,
                    entityId,
                    sanitizedOutcome,
                    ipAddress,
                    sanitizedDesc,
                    sanitizedDetails
            );

            return auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log record [action={}, entity={}]: {}", action, entityName, e.getMessage());
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logSuccess(User actor, String action, String entityName, Long entityId, String description) {
        return logEvent(actor, action, entityName, entityId, "SUCCESS", null, description, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logFailure(User actor, String action, String entityName, Long entityId, String failureReason) {
        return logEvent(actor, action, entityName, entityId, "FAILURE", null, "Failed: " + failureReason, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog logChange(User actor, String action, String entityName, Long entityId, Map<String, Object> oldValues, Map<String, Object> newValues, String description) {
        String changesJson = buildFieldDiffJson(oldValues, newValues);
        return logEvent(actor, action, entityName, entityId, "SUCCESS", null, description, changesJson);
    }

    public String buildFieldDiffJson(Map<String, Object> oldState, Map<String, Object> newState) {
        if (oldState == null || newState == null) return null;
        Map<String, Map<String, Object>> diffMap = new LinkedHashMap<>();

        for (String key : newState.keySet()) {
            if (isSensitiveField(key)) continue;

            Object oldVal = oldState.get(key);
            Object newVal = newState.get(key);

            if (!Objects.equals(oldVal, newVal)) {
                Map<String, Object> fieldDiff = new LinkedHashMap<>();
                fieldDiff.put("old", oldVal != null ? oldVal.toString() : null);
                fieldDiff.put("new", newVal != null ? newVal.toString() : null);
                diffMap.put(key, fieldDiff);
            }
        }

        if (diffMap.isEmpty()) return null;

        try {
            return objectMapper.writeValueAsString(diffMap);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) return false;
        String clean = fieldName.toLowerCase().replaceAll("[_-]", "");
        return SENSITIVE_FIELDS.contains(clean);
    }

    public String redactSensitiveJson(String input) {
        if (input == null || input.isBlank()) return input;
        try {
            Object obj = objectMapper.readValue(input, Object.class);
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                Map<String, Object> redacted = redactMap(map);
                return objectMapper.writeValueAsString(redacted);
            }
            return input;
        } catch (Exception e) {
            // Not valid JSON object, perform regex string replacement for sensitive terms
            String result = input;
            for (String sensitive : SENSITIVE_FIELDS) {
                result = result.replaceAll("(?i)\"" + sensitive + "\":\\s*\"[^\"]*\"", "\"" + sensitive + "\":\"[REDACTED]\"");
            }
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> redactMap(Map<?, ?> inputMap) {
        Map<String, Object> redacted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : inputMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (isSensitiveField(key)) {
                redacted.put(key, "[REDACTED]");
            } else if (value instanceof Map) {
                redacted.put(key, redactMap((Map<?, ?>) value));
            } else if (value instanceof List) {
                List<Object> list = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        list.add(redactMap((Map<?, ?>) item));
                    } else {
                        list.add(item);
                    }
                }
                redacted.put(key, list);
            } else {
                redacted.put(key, value);
            }
        }
        return redacted;
    }

    public String sanitizeText(String input) {
        if (input == null) return null;
        // Strip out control characters to prevent log injection
        return input.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
