
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.ChatConversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ChatConversationRepository
        extends JpaRepository<ChatConversation, Long> {

    // Get all conversations belonging to a user, newest first
    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(
            Long userId
    );


    // Get conversations belonging to a specific semester
    List<ChatConversation>
            findByUserIdAndSemesterIdOrderByUpdatedAtDesc(
                    Long userId,
                    Long semesterId
            );
}
