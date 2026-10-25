
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.ChatConversation;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;

import com.smartsem.smartsem.service.ChatConversationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/conversations")
public class ChatConversationController {

    private final ChatConversationService conversationService;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;


    public ChatConversationController(
            ChatConversationService conversationService,
            UserRepository userRepository,
            SemesterRepository semesterRepository) {

        this.conversationService = conversationService;
        this.userRepository = userRepository;
        this.semesterRepository = semesterRepository;
    }


    // GET: Retrieve the logged-in user's conversations
    @GetMapping
    public ResponseEntity<?> getConversations(
            @RequestParam(required = false) Long semesterId,
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        List<ChatConversation> conversations;

        if (semesterId != null) {

            boolean ownsSemester = semesterRepository
                    .findById(semesterId)
                    .map(semester ->
                            semester.getUserId().equals(user.getId()))
                    .orElse(false);

            if (!ownsSemester) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Semester not found"));
            }

            conversations = conversationService
                    .getConversationsBySemester(
                            user.getId(),
                            semesterId
                    );

        } else {

            conversations = conversationService
                    .getConversationsByUser(user.getId());
        }

        return ResponseEntity.ok(conversations);
    }


    // POST: Create a new conversation
    @PostMapping
    public ResponseEntity<?> createConversation(
            @RequestBody ChatConversation conversation,
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found"));
        }

        if (conversation.getSemesterId() != null) {

            boolean ownsSemester = semesterRepository
                    .findById(conversation.getSemesterId())
                    .map(semester ->
                            semester.getUserId().equals(user.getId()))
                    .orElse(false);

            if (!ownsSemester) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Semester not found"));
            }
        }

        // Never accept userId from the request body
        conversation.setUserId(user.getId());

        ChatConversation savedConversation =
                conversationService.createConversation(conversation);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedConversation);
    }
}
