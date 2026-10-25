
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.Notification;
import com.smartsem.smartsem.repository.NotificationRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(
            Long userId,
            String title,
            String message,
            String type) {

        return createNotification(userId, title, message, type, null);
    }

    @Transactional
    public Notification createNotification(
            Long userId,
            String title,
            String message,
            String type,
            Long assignmentId) {

        if (userId == null
                || title == null || title.isBlank()
                || message == null || message.isBlank()
                || type == null || type.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "User, title, message and type are required"
            );
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setAssignmentId(assignmentId);
        notification.setTitle(title.trim());
        notification.setMessage(message.trim());
        notification.setType(type.trim().toUpperCase());

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository
                .countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found"
                ));

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        List<Notification> unread =
                notificationRepository
                        .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        );

        unread.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unread);

        return unread.size();
    }

    @Transactional(readOnly = true)
    public boolean notificationExists(
            Long userId,
            String type,
            String message) {

        return notificationRepository
                .existsByUserIdAndTypeAndMessage(userId, type, message);
    }

    @Transactional(readOnly = true)
    public boolean assignmentReminderExists(
            Long userId,
            Long assignmentId) {

        return notificationRepository
                .existsByUserIdAndTypeAndAssignmentId(
                        userId,
                        "ASSIGNMENT_REMINDER",
                        assignmentId
                );
    }
}
