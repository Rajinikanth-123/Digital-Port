package com.portshipping.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class UserProfileDto {

    public record UserProfileResponse(
            Long id,
            String name,
            String fullName,
            String email,
            String phoneNumber,
            String role,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Name cannot be blank") String name,
            String fullName,
            String phoneNumber,
            @Email(message = "Valid email is required") @NotBlank(message = "Email cannot be blank") String email
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 8, message = "New password must be at least 8 characters long") String newPassword,
            @NotBlank(message = "Password confirmation is required") String confirmPassword
    ) {}

    public record UserPreferencesDto(
            String theme,
            String language,
            String timezone,
            Boolean notifyShipments,
            Boolean notifyVessels,
            Boolean notifyCustoms,
            Boolean notifyInvoices,
            Boolean notifySystem
    ) {}

    public record AccountSecurityResponse(
            Long id,
            String email,
            String role,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt,
            String passwordLastChanged
    ) {}

    public record DeactivateAccountRequest(
            @NotBlank(message = "Current password is required for confirmation") String currentPassword,
            String reason
    ) {}
}
