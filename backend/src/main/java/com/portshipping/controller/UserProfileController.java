package com.portshipping.controller;

import com.portshipping.dto.UserProfileDto.*;
import com.portshipping.entity.User;
import com.portshipping.exception.NotFoundException;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    public UserProfileController(
            UserProfileService userProfileService,
            UserRepository userRepository,
            UserDetailsService userDetailsService) {
        this.userProfileService = userProfileService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        User currentUser = resolveRequiredUser(authentication);
        return ResponseEntity.ok(userProfileService.getProfile(currentUser));
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = resolveRequiredUser(authentication);
        UserProfileResponse updated = userProfileService.updateProfile(currentUser, request);

        // If email changed, refresh security context authentication
        if (!currentUser.getEmail().equalsIgnoreCase(updated.email())) {
            refreshSecurityContext(updated.email());
        }

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = resolveRequiredUser(authentication);
        userProfileService.changePassword(currentUser, request);
        refreshSecurityContext(currentUser.getEmail());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> getPreferences(Authentication authentication) {
        User currentUser = resolveRequiredUser(authentication);
        return ResponseEntity.ok(userProfileService.getPreferences(currentUser));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesDto> updatePreferences(
            Authentication authentication,
            @RequestBody UserPreferencesDto request) {
        User currentUser = resolveRequiredUser(authentication);
        return ResponseEntity.ok(userProfileService.updatePreferences(currentUser, request));
    }

    @GetMapping("/security")
    public ResponseEntity<AccountSecurityResponse> getSecurity(Authentication authentication) {
        User currentUser = resolveRequiredUser(authentication);
        return ResponseEntity.ok(userProfileService.getSecurityInfo(currentUser));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            Authentication authentication,
            HttpServletRequest servletRequest,
            @Valid @RequestBody DeactivateAccountRequest request) {
        User currentUser = resolveRequiredUser(authentication);
        userProfileService.deactivateAccount(currentUser, request);

        // Invalidate session on self-deactivation
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of("message", "Account deactivated successfully. You have been logged out."));
    }

    private User resolveRequiredUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("User is not authenticated.");
        }
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user profile not found."));
    }

    private void refreshSecurityContext(String email) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } catch (Exception ignored) {
            // Context refresh attempt ignored if user details service cannot resolve immediately
        }
    }
}
