package com.portshipping.service;

import com.portshipping.dto.UserProfileDto.*;
import com.portshipping.entity.User;
import com.portshipping.entity.UserProfilePreference;
import com.portshipping.exception.ConflictException;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.UserProfilePreferenceRepository;
import com.portshipping.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfilePreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserProfileService(
            UserRepository userRepository,
            UserProfilePreferenceRepository preferenceRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User currentUser) {
        User user = fetchLatestUser(currentUser);
        return toProfileResponse(user);
    }

    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = fetchLatestUser(currentUser);

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("name", user.getName());
        oldValues.put("fullName", user.getFullName());
        oldValues.put("email", user.getEmail());
        oldValues.put("phoneNumber", user.getPhoneNumber());

        String newEmail = request.email() != null ? request.email().toLowerCase().trim() : "";
        if (!newEmail.isBlank() && !newEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new ConflictException("Email '" + newEmail + "' is already registered.");
            }
            user.setEmail(newEmail);
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(request.phoneNumber().trim());
        }

        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("name", saved.getName());
        newValues.put("fullName", saved.getFullName());
        newValues.put("email", saved.getEmail());
        newValues.put("phoneNumber", saved.getPhoneNumber());

        auditLogService.logChange(saved, "USER_PROFILE_UPDATED", "User", saved.getId(), oldValues, newValues, "Updated profile details for " + saved.getEmail());
        return toProfileResponse(saved);
    }

    public void changePassword(User currentUser, ChangePasswordRequest request) {
        User user = fetchLatestUser(currentUser);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.setPassword(encodedPassword);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.logSuccess(user, "PASSWORD_CHANGED", "User", user.getId(), "User changed password successfully for " + user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(User currentUser) {
        User user = fetchLatestUser(currentUser);
        UserProfilePreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> preferenceRepository.save(new UserProfilePreference(user)));
        return toPreferencesDto(pref);
    }

    public UserPreferencesDto updatePreferences(User currentUser, UserPreferencesDto request) {
        User user = fetchLatestUser(currentUser);
        UserProfilePreference pref = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserProfilePreference(user));

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("theme", pref.getTheme());
        oldValues.put("language", pref.getLanguage());
        oldValues.put("timezone", pref.getTimezone());
        oldValues.put("notifyShipments", pref.isNotifyShipments());
        oldValues.put("notifyVessels", pref.isNotifyVessels());
        oldValues.put("notifyCustoms", pref.isNotifyCustoms());
        oldValues.put("notifyInvoices", pref.isNotifyInvoices());
        oldValues.put("notifySystem", pref.isNotifySystem());

        if (request.theme() != null && !request.theme().isBlank()) {
            String cleanTheme = request.theme().toLowerCase().trim();
            if ("dark".equals(cleanTheme) || "light".equals(cleanTheme) || "system".equals(cleanTheme)) {
                pref.setTheme(cleanTheme);
            }
        }

        if (request.language() != null && !request.language().isBlank()) {
            pref.setLanguage(request.language().trim());
        }

        if (request.timezone() != null && !request.timezone().isBlank()) {
            pref.setTimezone(request.timezone().trim());
        }

        if (request.notifyShipments() != null) pref.setNotifyShipments(request.notifyShipments());
        if (request.notifyVessels() != null) pref.setNotifyVessels(request.notifyVessels());
        if (request.notifyCustoms() != null) pref.setNotifyCustoms(request.notifyCustoms());
        if (request.notifyInvoices() != null) pref.setNotifyInvoices(request.notifyInvoices());
        if (request.notifySystem() != null) pref.setNotifySystem(request.notifySystem());

        UserProfilePreference saved = preferenceRepository.save(pref);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("theme", saved.getTheme());
        newValues.put("language", saved.getLanguage());
        newValues.put("timezone", saved.getTimezone());
        newValues.put("notifyShipments", saved.isNotifyShipments());
        newValues.put("notifyVessels", saved.isNotifyVessels());
        newValues.put("notifyCustoms", saved.isNotifyCustoms());
        newValues.put("notifyInvoices", saved.isNotifyInvoices());
        newValues.put("notifySystem", saved.isNotifySystem());

        auditLogService.logChange(user, "USER_PREFERENCES_UPDATED", "UserProfilePreference", saved.getId(), oldValues, newValues, "Updated preferences for " + user.getEmail());
        return toPreferencesDto(saved);
    }

    @Transactional(readOnly = true)
    public AccountSecurityResponse getSecurityInfo(User currentUser) {
        User user = fetchLatestUser(currentUser);
        return new AccountSecurityResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : user.getCreatedAt().toString()
        );
    }

    public void deactivateAccount(User currentUser, DeactivateAccountRequest request) {
        User user = fetchLatestUser(currentUser);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole()) && user.isEnabled()) {
            long activeAdminCount = userRepository.countByRoleIgnoreCaseAndEnabledTrue("ADMIN");
            if (activeAdminCount <= 1) {
                throw new ConflictException("Cannot deactivate the final active administrator.");
            }
        }

        user.setEnabled(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditLogService.logSuccess(user, "ACCOUNT_DEACTIVATED", "User", user.getId(), "Self-service account deactivation for " + user.getEmail() + ". Reason: " + (request.reason() != null ? request.reason() : "N/A"));
    }

    private User fetchLatestUser(User currentUser) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new NotFoundException("Authenticated user principal not found.");
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("User not found for ID: " + currentUser.getId()));
    }

    private UserProfileResponse toProfileResponse(User u) {
        return new UserProfileResponse(
                u.getId(),
                u.getName(),
                u.getFullName() != null ? u.getFullName() : u.getName(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getRole(),
                u.isEnabled(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }

    private UserPreferencesDto toPreferencesDto(UserProfilePreference pref) {
        return new UserPreferencesDto(
                pref.getTheme(),
                pref.getLanguage(),
                pref.getTimezone(),
                pref.isNotifyShipments(),
                pref.isNotifyVessels(),
                pref.isNotifyCustoms(),
                pref.isNotifyInvoices(),
                pref.isNotifySystem()
        );
    }
}
