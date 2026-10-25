
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.AiActionLog;
import com.smartsem.smartsem.repository.AiActionLogRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AiActionLogService {

    private final AiActionLogRepository actionLogRepository;


    public AiActionLogService(
            AiActionLogRepository actionLogRepository) {

        this.actionLogRepository = actionLogRepository;
    }


    // Get a user's AI action history
    public List<AiActionLog> getLogsByUser(Long userId) {

        return actionLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }


    // Filter a user's AI action history by action type
    public List<AiActionLog> getLogsByActionType(
            Long userId,
            String actionType) {

        return actionLogRepository
                .findByUserIdAndActionTypeOrderByCreatedAtDesc(
                        userId,
                        actionType
                );
    }


    // Record a new AI action
    public AiActionLog createLog(AiActionLog log) {

        return actionLogRepository.save(log);
    }
}
