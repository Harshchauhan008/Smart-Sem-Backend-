
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.ChatMessage;
import com.smartsem.smartsem.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<ChatMessage> getMessagesByConversation(Long conversationId) {
        return chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }
}
