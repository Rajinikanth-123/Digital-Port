package com.portshipping.controller;

import com.portshipping.entity.User;
import com.portshipping.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public record UserResponse(
            Long id,
            String name,
            String fullName,
            String email,
            String role,
            boolean enabled,
            Instant createdAt
    ) {}

    public record CreateUserRequest(
            @NotBlank(message = "Name is required") String name,
            String fullName,
            @Email(message = "Valid email is required") @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,
            String role,
            Boolean enabled
    ) {}

    public record UpdateUserRequest(
            String name,
            String fullName,
            @Email(message = "Invalid email format") String email,
            Boolean enabled
    ) {}

    public record RoleUpdateRequest(
            @NotBlank(message = "Role is required") String role
    ) {}

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getFullName() != null ? u.getFullName() : u.getName(),
                u.getEmail(),
                u.getRole(),
                u.isEnabled(),
                u.getCreatedAt()
        );
    }

    @GetMapping
    public Page<UserResponse> all(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        Page<User> usersPage = userService.page(search, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        List<UserResponse> safeUsers = usersPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(safeUsers, usersPage.getPageable(), usersPage.getTotalElements());
    }

    @GetMapping("/{id}")
    public UserResponse one(@PathVariable Long id) {
        User u = userService.findById(id);
        return toResponse(u);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest body) {
        User u = userService.createUser(
                body.name(),
                body.fullName(),
                body.email(),
                body.password(),
                body.role(),
                body.enabled()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(u));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest body) {
        User u = userService.updateUser(id, body.name(), body.fullName(), body.email(), body.enabled());
        return toResponse(u);
    }

    @PutMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest body) {
        User u = userService.updateRole(id, body.role());
        return toResponse(u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

