
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AiActionLog;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.AiActionLogService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/ai-logs")
public class AiActionLogController {

    private final AiActionLogService actionLogService;
    private final UserRepository userRepository;


    public AiActionLogController(
            AiActionLogService actionLogService,
            UserRepository userRepository) {

        this.actionLogService = actionLogService;
        this.userRepository = userRepository;
    }


    // GET: Retrieve the logged-in user's AI action history
    @GetMapping
    public ResponseEntity<List<AiActionLog>> getLogs(
            @RequestParam(required = false) String type,
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found"));

        List<AiActionLog> logs;

        if (type != null && !type.isBlank()) {
            logs = actionLogService.getLogsByActionType(
                    user.getId(),
                    type.trim().toUpperCase()
            );
        } else {
            logs = actionLogService.getLogsByUser(user.getId());
        }

        return ResponseEntity.ok(logs);
    }
}
