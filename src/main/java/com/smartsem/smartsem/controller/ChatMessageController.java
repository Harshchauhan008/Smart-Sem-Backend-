
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.ChatConversation;
import com.smartsem.smartsem.entity.ChatMessage;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.ChatConversationRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.ChatMessageService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/conversations/{conversationId}/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ChatMessageController(
            ChatMessageService chatMessageService,
            ChatConversationRepository conversationRepository,
            UserRepository userRepository) {
        this.chatMessageService = chatMessageService;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    private ChatConversation getOwnedConversation(
            Long conversationId,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        ChatConversation conversation =
                conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access this conversation");
        }

        return conversation;
    }

    @GetMapping
    public List<ChatMessage> getMessages(
            @PathVariable Long conversationId,
            Authentication authentication) {

        getOwnedConversation(conversationId, authentication);

        return chatMessageService
                .getMessagesByConversation(conversationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessage createMessage(
            @PathVariable Long conversationId,
            @RequestBody ChatMessage request,
            Authentication authentication) {

        getOwnedConversation(conversationId, authentication);

        if (request.getMessageText() == null
                || request.getMessageText().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Message text is required");
        }

        if (request.getRole() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Message role is required");
        }

        String role = request.getRole().trim().toUpperCase();

        if (!role.equals("USER") && !role.equals("ASSISTANT")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid message role");
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setMessageText(request.getMessageText().trim());
        message.setIntent(request.getIntent());

        return chatMessageService.saveMessage(message);
    }
}
