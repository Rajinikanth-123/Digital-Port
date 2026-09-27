package com.portshipping.service;

import com.portshipping.entity.Notification;
import com.portshipping.entity.User;
import com.portshipping.repository.NotificationRepository;
import com.portshipping.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.portshipping.entity.UserProfilePreference;
import com.portshipping.repository.UserProfilePreferenceRepository;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserProfilePreferenceRepository preferenceRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            UserProfilePreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public Notification sendNotification(User user, String title, String message, String type, String entityName, Long entityId) {
        return sendNotification(user, title, message, type, entityName, entityId, "NORMAL");
    }

    public Notification sendNotification(User user, String title, String message, String type, String entityName, Long entityId, String priority) {
        Long userId = user != null ? user.getId() : null;

        // Check user notification preferences if user is specific and priority is not URGENT
        if (user != null && userId != null && !"URGENT".equalsIgnoreCase(priority)) {
            boolean enabled = isNotificationCategoryEnabled(userId, type);
            if (!enabled) {
                return null; // Suppressed by user preference
            }
        }

        Instant cutoff = Instant.now().minus(30, ChronoUnit.SECONDS);

        // Deduplication check: prevent identical notification within 30s
        if (entityName != null && entityId != null &&
            notificationRepository.existsDuplicate(userId, title, entityName, entityId, cutoff)) {
            return null;
        }

        Notification notification = new Notification(user, title, message, type, entityName, entityId, priority);
        return notificationRepository.save(notification);
    }

    private boolean isNotificationCategoryEnabled(Long userId, String type) {
        if (type == null) return true;
        return preferenceRepository.findByUserId(userId).map(pref -> {
            String upper = type.toUpperCase().trim();
            switch (upper) {
                case "SHIPMENT":
                    return pref.isNotifyShipments();
                case "VESSEL":
                case "BERTH":
                    return pref.isNotifyVessels();
                case "CUSTOMS":
                    return pref.isNotifyCustoms();
                case "INVOICE":
                case "PAYMENT":
                    return pref.isNotifyInvoices();
                case "SYSTEM":
                    return pref.isNotifySystem();
                default:
                    return true;
            }
        }).orElse(true);
    }

    public List<Notification> notifyUsersByRole(List<String> roles, String title, String message, String type, String entityName, Long entityId) {
        return notifyUsersByRole(roles, title, message, type, entityName, entityId, "NORMAL");
    }

    public List<Notification> notifyUsersByRole(List<String> roles, String title, String message, String type, String entityName, Long entityId, String priority) {
        List<User> targetUsers = userRepository.findByRoleIgnoreCaseIn(roles);
        List<Notification> created = new ArrayList<>();

        if (targetUsers.isEmpty()) {
            // Broadcast system alert if no specific users found
            Notification n = sendNotification(null, title, message, type, entityName, entityId, priority);
            if (n != null) created.add(n);
        } else {
            for (User u : targetUsers) {
                Notification n = sendNotification(u, title, message, type, entityName, entityId, priority);
                if (n != null) created.add(n);
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, Boolean unreadOnly, String category, Boolean archived, Pageable pageable) {
        Long userId = user != null ? user.getId() : null;
        Boolean isArchived = archived != null ? archived : false;
        String cat = (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) ? category.trim() : null;

        return notificationRepository.findFilteredForUser(userId, unreadOnly, cat, isArchived, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return getUserNotifications(user, false, null, false, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        Long userId = user != null ? user.getId() : null;
        return notificationRepository.countUnreadForUser(userId);
    }

    public Notification markAsRead(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: #" + id));

        validateOwnership(notification, user);

        notification.setReadStatus(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(User user) {
        Long userId = user != null ? user.getId() : null;
        List<Notification> unread = notificationRepository.findAllUnreadForUser(userId);
        for (Notification n : unread) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(unread);
    }

    public Notification archiveNotification(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: #" + id));

        validateOwnership(notification, user);

        notification.setArchived(true);
        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found: #" + id));

        validateOwnership(notification, user);

        notificationRepository.delete(notification);
    }

    private void validateOwnership(Notification notification, User user) {
        if (user == null) return;
        Long currentUserId = user.getId();
        if (notification.getUser() != null && !notification.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to notification #" + notification.getId());
        }
    }
}
