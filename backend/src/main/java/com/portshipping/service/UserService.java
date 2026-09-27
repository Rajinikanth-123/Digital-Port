package com.portshipping.service;

import com.portshipping.entity.User;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.AuditLogRepository;
import com.portshipping.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditLogRepository auditLogs;
    private final AuditLogService auditLogService;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditLogRepository auditLogs, AuditLogService auditLogService) {
        this.users = users;
        this.encoder = encoder;
        this.auditLogs = auditLogs;
        this.auditLogService = auditLogService;
    }

    public User register(String name, String email, String password) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + email + "' is already registered.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }
        User user = new User(name, email.toLowerCase().trim(), encoder.encode(password));
        User saved = users.save(user);
        auditLogService.logSuccess(saved, "USER_REGISTERED", "User", saved.getId(), "User self-registered: " + saved.getEmail());
        return saved;
    }

    @Transactional
    public User createUser(String name, String fullName, String email, String password, String role, Boolean enabled) {
        String cleanEmail = email != null ? email.toLowerCase().trim() : "";
        if (users.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ConflictException("Email '" + cleanEmail + "' is already registered.");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }

        User user = new User(name, cleanEmail, encoder.encode(password));
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        user.setRole(validateRole(role));
        user.setEnabled(enabled != null ? enabled : true);

        User saved = users.save(user);
        auditLogService.logSuccess(null, "USER_CREATED", "User", saved.getId(), "Admin created user account: " + saved.getEmail() + " with role " + saved.getRole());
        return saved;
    }

    public User find(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User '" + email + "' not found."));
    }

    public Optional<User> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email);
    }

    public User findById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found."));
    }

    @Transactional
    public User updateUser(Long userId, String name, String fullName, String email, Boolean enabled) {
        User u = findById(userId);

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("name", u.getName());
        oldValues.put("fullName", u.getFullName());
        oldValues.put("email", u.getEmail());
        oldValues.put("enabled", u.isEnabled());

        if (email != null && !email.isBlank() && !email.equalsIgnoreCase(u.getEmail())) {
            String cleanEmail = email.toLowerCase().trim();
            if (users.existsByEmailIgnoreCase(cleanEmail)) {
                throw new ConflictException("Email '" + cleanEmail + "' is already registered.");
            }
            u.setEmail(cleanEmail);
        }

        if (name != null && !name.isBlank()) {
            u.setName(name.trim());
        }
        if (fullName != null) {
            u.setFullName(fullName.trim());
        }

        if (enabled != null && enabled != u.isEnabled()) {
            if (!enabled && u.isEnabled() && "ADMIN".equalsIgnoreCase(u.getRole())) {
                long activeAdminCount = users.countByRoleIgnoreCaseAndEnabledTrue("ADMIN");
                if (activeAdminCount <= 1) {
                    throw new ConflictException("Cannot deactivate the final active administrator.");
                }
            }
            u.setEnabled(enabled);
        }

        u.setUpdatedAt(Instant.now());
        User saved = users.save(u);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", saved.getName());
        newValues.put("fullName", saved.getFullName());
        newValues.put("email", saved.getEmail());
        newValues.put("enabled", saved.isEnabled());

        auditLogService.logChange(null, "USER_UPDATED", "User", saved.getId(), oldValues, newValues, "Updated user details for " + saved.getEmail());
        return saved;
    }

    @Transactional
    public User updateRole(Long userId, String newRole) {
        User u = findById(userId);
        String formattedRole = validateRole(newRole);

        Map<String, Object> oldValues = Map.of("role", u.getRole());

        if ("ADMIN".equalsIgnoreCase(u.getRole()) && !formattedRole.equals("ADMIN") && u.isEnabled()) {
            long activeAdminCount = users.countByRoleIgnoreCaseAndEnabledTrue("ADMIN");
            if (activeAdminCount <= 1) {
                throw new ConflictException("Cannot remove administrative role from the final active administrator.");
            }
        }

        u.setRole(formattedRole);
        u.setUpdatedAt(Instant.now());
        User saved = users.save(u);

        Map<String, Object> newValues = Map.of("role", saved.getRole());

        auditLogService.logChange(null, "ROLE_UPDATED", "User", saved.getId(), oldValues, newValues, "Updated role to " + formattedRole + " for " + saved.getEmail());
        return saved;
    }

    @Transactional
    public void delete(Long userId) {
        User u = findById(userId);
        if ("ADMIN".equalsIgnoreCase(u.getRole()) && u.isEnabled()) {
            long activeAdminCount = users.countByRoleIgnoreCaseAndEnabledTrue("ADMIN");
            if (activeAdminCount <= 1) {
                throw new ConflictException("Cannot delete the final active administrator.");
            }
        }
        users.delete(u);
        auditLogService.logSuccess(null, "USER_DELETED", "User", userId, "Deleted user account: " + u.getEmail());
    }

    public List<User> listAll() { return users.findAll(); }
    public Page<User> listAll(Pageable pageable) { return users.findAll(pageable); }
    public Page<User> page(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            return users.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(term, term, pageable);
        }
        return users.findAll(pageable);
    }
    public Page<User> search(String term, Pageable pageable) {
        if (term != null && !term.isBlank()) {
            String clean = term.trim();
            return users.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(clean, clean, pageable);
        }
        return users.findAll(pageable);
    }

    private String validateRole(String role) {
        if (role == null || role.isBlank()) return "VIEWER";
        String roleStr = role.toUpperCase().trim();
        if (roleStr.startsWith("ROLE_")) {
            roleStr = roleStr.substring(5);
        }
        if (!roleStr.equals("ADMIN") && !roleStr.equals("PORT_MANAGER") &&
            !roleStr.equals("SHIPPING_AGENT") && !roleStr.equals("VIEWER")) {
            throw new IllegalArgumentException("Invalid role name: " + role);
        }
        return roleStr;
    }
}
