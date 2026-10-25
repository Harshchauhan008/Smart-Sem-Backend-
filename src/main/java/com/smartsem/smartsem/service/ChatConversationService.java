
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.ChatConversation;
import com.smartsem.smartsem.repository.ChatConversationRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ChatConversationService {

    private final ChatConversationRepository conversationRepository;


    public ChatConversationService(
            ChatConversationRepository conversationRepository) {

        this.conversationRepository = conversationRepository;
    }


    // Get all conversations for a user
    public List<ChatConversation> getConversationsByUser(
            Long userId) {

        return conversationRepository
                .findByUserIdOrderByUpdatedAtDesc(userId);
    }


    // Get conversations for a particular semester
    public List<ChatConversation> getConversationsBySemester(
            Long userId,
            Long semesterId) {

        return conversationRepository
                .findByUserIdAndSemesterIdOrderByUpdatedAtDesc(
                        userId,
                        semesterId
                );
    }


    // Create a new conversation
    public ChatConversation createConversation(
            ChatConversation conversation) {

        return conversationRepository.save(conversation);
    }
}
