
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Notification> findByIdAndUserId(
            Long id,
            Long userId
    );

    long countByUserIdAndReadFalse(Long userId);


    boolean existsByUserIdAndTypeAndMessage(
        Long userId, String type, String message);

boolean existsByUserIdAndTypeAndAssignmentId(
        Long userId, String type, Long assignmentId);

}
