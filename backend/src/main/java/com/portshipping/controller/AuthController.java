package com.portshipping.controller;

import com.portshipping.entity.User;
import com.portshipping.service.AuditLogService;
import com.portshipping.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService service;
    private final AuthenticationManager manager;
    private final AuditLogService auditLogService;

    public AuthController(UserService s, AuthenticationConfiguration config, AuditLogService auditLogService) throws Exception {
        this.service = s;
        this.manager = config.getAuthenticationManager();
        this.auditLogService = auditLogService;
    }

    public record Credentials(@NotBlank String email, @NotBlank String password) {}
    public record Registration(@NotBlank String name, @Email @NotBlank String email, @NotBlank String password) {}
    public record UserResponse(Long id, String name, String email, String role) {}

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody Registration r, HttpServletRequest request) {
        User u = service.register(r.name(), r.email(), r.password());
        auditLogService.logEvent(u, "USER_REGISTERED", "User", u.getId(), "SUCCESS", getClientIp(request), "User self-registered: " + u.getEmail(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody Credentials c, HttpServletRequest request) {
        String cleanEmail = c.email() != null ? c.email().trim().toLowerCase() : "";
        String ipAddress = getClientIp(request);
        try {
            Authentication authentication = manager.authenticate(
                    new UsernamePasswordAuthenticationToken(cleanEmail, c.password())
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
            );
            User u = service.find(cleanEmail);
            auditLogService.logEvent(u, "LOGIN_SUCCESS", "Auth", u.getId(), "SUCCESS", ipAddress, "User logged in: " + u.getEmail(), null);
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
        } catch (Exception e) {
            User attemptedUser = null;
            try { attemptedUser = service.find(cleanEmail); } catch (Exception ignored) {}
            auditLogService.logEvent(attemptedUser, "LOGIN_FAILED", "Auth", attemptedUser != null ? attemptedUser.getId() : null, "FAILURE", ipAddress, "Failed login attempt for: " + cleanEmail, null);
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid email or password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        User u = auditLogService.getCurrentAuthenticatedUser();
        String ipAddress = getClientIp(request);
        if (u != null) {
            auditLogService.logEvent(u, "LOGOUT", "Auth", u.getId(), "SUCCESS", ipAddress, "User logged out: " + u.getEmail(), null);
        }
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        String email = authentication.getName();
        try {
            User u = service.find(email);
            return ResponseEntity.ok(new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("username", email, "role", authentication.getAuthorities().toString()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "127.0.0.1";
    }
}
