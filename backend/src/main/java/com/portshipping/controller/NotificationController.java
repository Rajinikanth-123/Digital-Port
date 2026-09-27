package com.portshipping.controller;

import com.portshipping.entity.Notification;
import com.portshipping.entity.User;
import com.portshipping.repository.UserRepository;
import com.portshipping.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @RequestParam(required = false) Boolean unreadOnly,
                                                              @RequestParam(required = false) String category,
                                                              @RequestParam(required = false) Boolean archived,
                                                              Authentication authentication) {
        User currentUser = resolveUser(authentication);
        Page<Notification> result = notificationService.getUserNotifications(currentUser, unreadOnly, category, archived, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication authentication) {
        User currentUser = resolveUser(authentication);
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        Notification updated = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(updated);
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        User currentUser = resolveUser(authentication);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read."));
    }

    @RequestMapping(value = "/{id}/archive", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Notification> archiveNotification(@PathVariable Long id, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        Notification updated = notificationService.archiveNotification(id, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long id, Authentication authentication) {
        User currentUser = resolveUser(authentication);
        notificationService.deleteNotification(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Notification #" + id + " deleted successfully."));
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }
}
