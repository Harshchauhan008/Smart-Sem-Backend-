
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.Notification;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Notification> getNotifications(
            Authentication authentication) {
        User user = getLoggedInUser(authentication);
        return notificationService.getNotifications(user.getId());
    }

    @GetMapping("/unread")
    public List<Notification> getUnreadNotifications(
            Authentication authentication) {
        User user = getLoggedInUser(authentication);
        return notificationService.getUnreadNotifications(user.getId());
    }

    @GetMapping("/unread/count")
    public Map<String, Long> countUnreadNotifications(
            Authentication authentication) {
        User user = getLoggedInUser(authentication);

        return Map.of(
                "unreadCount",
                notificationService.countUnreadNotifications(user.getId())
        );
    }

    @PatchMapping("/{notificationId}/read")
    public Notification markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication) {
        User user = getLoggedInUser(authentication);

        return notificationService.markAsRead(
                notificationId, user.getId()
        );
    }

    @PatchMapping("/read-all")
    public Map<String, Integer> markAllAsRead(
            Authentication authentication) {
        User user = getLoggedInUser(authentication);

        int updated = notificationService.markAllAsRead(user.getId());

        return Map.of("updatedCount", updated);
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));
    }
}
