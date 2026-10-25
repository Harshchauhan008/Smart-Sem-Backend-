
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.AiActionLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AiActionLogRepository
        extends JpaRepository<AiActionLog, Long> {

    // Get all AI action logs for a user, newest first
    List<AiActionLog> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );


    // Get logs for a specific action type
    List<AiActionLog> findByUserIdAndActionTypeOrderByCreatedAtDesc(
            Long userId,
            String actionType
    );
}
