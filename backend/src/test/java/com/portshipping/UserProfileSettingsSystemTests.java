package com.portshipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portshipping.dto.UserProfileDto.*;
import com.portshipping.entity.Notification;
import com.portshipping.entity.User;
import com.portshipping.entity.UserProfilePreference;
import com.portshipping.repository.AuditLogRepository;
import com.portshipping.repository.NotificationRepository;
import com.portshipping.repository.UserProfilePreferenceRepository;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.NotificationService;
import com.portshipping.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserProfileSettingsSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfilePreferenceRepository preferenceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        preferenceRepository.deleteAll();
        auditLogRepository.deleteAll();

        testUser = userRepository.findByEmailIgnoreCase("profile_user@portshipping.com").orElseGet(() -> {
            User u = new User("Profile Test User", "profile_user@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setFullName("Profile Test User Full");
            u.setRole("VIEWER");
            return userRepository.save(u);
        });

        adminUser = userRepository.findByEmailIgnoreCase("admin_profile@portshipping.com").orElseGet(() -> {
            User u = new User("Profile Admin", "admin_profile@portshipping.com", passwordEncoder.encode("Password123!"));
            u.setRole("ADMIN");
            return userRepository.save(u);
        });
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testGetMyProfile_AuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("profile_user@portshipping.com"))
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testUpdateMyProfile_Success() throws Exception {
        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "Updated Profile Name",
                "Updated Profile Full Name",
                "+1-555-0199",
                "profile_user_updated@portshipping.com"
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Profile Name"))
                .andExpect(jsonPath("$.phoneNumber").value("+1-555-0199"))
                .andExpect(jsonPath("$.email").value("profile_user_updated@portshipping.com"));

        // Verify audit log entry
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> "USER_PROFILE_UPDATED".equals(log.getAction())));
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testUpdateMyProfile_DuplicateEmail_Conflict() throws Exception {
        UpdateProfileRequest updateReq = new UpdateProfileRequest(
                "Profile Test User",
                "Profile Test User Full",
                null,
                "admin_profile@portshipping.com" // Conflicts with adminUser email
        );

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest passwordReq = new ChangePasswordRequest(
                "Password123!",
                "NewSecurePassword456!",
                "NewSecurePassword456!"
        );

        mockMvc.perform(post("/api/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordReq))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        // Verify password hash updated in DB
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePassword456!", updated.getPassword()));

        // Verify audit log entry without password logging
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(log -> "PASSWORD_CHANGED".equals(log.getAction())));
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testChangePassword_WrongCurrentPassword_BadRequest() throws Exception {
        ChangePasswordRequest passwordReq = new ChangePasswordRequest(
                "WrongCurrentPassword!",
                "NewSecurePassword456!",
                "NewSecurePassword456!"
        );

        mockMvc.perform(post("/api/users/me/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(passwordReq))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testGetAndSavePreferences() throws Exception {
        // Fetch default preferences
        mockMvc.perform(get("/api/users/me/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("light"))
                .andExpect(jsonPath("$.notifyShipments").value(true));

        // Update preferences
        UserPreferencesDto updatePref = new UserPreferencesDto(
                "dark",
                "en",
                "America/New_York",
                false,
                true,
                false,
                true,
                true
        );

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePref))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.notifyShipments").value(false))
                .andExpect(jsonPath("$.notifyCustoms").value(false));

        UserProfilePreference saved = preferenceRepository.findByUserId(testUser.getId()).orElseThrow();
        assertEquals("dark", saved.getTheme());
        assertFalse(saved.isNotifyShipments());
    }

    @Test
    void testNotificationSuppressionByPreferences() {
        // Disable SHIPMENT notifications for testUser
        UserProfilePreference pref = new UserProfilePreference(testUser);
        pref.setNotifyShipments(false);
        pref.setNotifyInvoices(true);
        preferenceRepository.save(pref);

        // Send SHIPMENT notification (should be suppressed / return null)
        Notification suppressed = notificationService.sendNotification(
                testUser, "Shipment Updated", "Shipment #101 departed", "SHIPMENT", "Shipment", 101L, "NORMAL"
        );
        assertNull(suppressed);

        // Send INVOICE notification (should be delivered)
        Notification delivered = notificationService.sendNotification(
                testUser, "Invoice Issued", "Invoice #INV-55 created", "INVOICE", "Invoice", 55L, "NORMAL"
        );
        assertNotNull(delivered);
        assertEquals("Invoice Issued", delivered.getTitle());
    }

    @Test
    @WithMockUser(username = "profile_user@portshipping.com", roles = {"VIEWER"})
    void testDeactivateAccount_Success() throws Exception {
        DeactivateAccountRequest req = new DeactivateAccountRequest("Password123!", "Personal choice");

        mockMvc.perform(post("/api/users/me/deactivate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk());

        User deactivated = userRepository.findById(testUser.getId()).orElseThrow();
        assertFalse(deactivated.isEnabled());
    }
}
