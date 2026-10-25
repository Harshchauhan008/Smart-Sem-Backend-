
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;


    @Column(nullable = false)
    private String role;


    @Column(name = "message_text", nullable = false, columnDefinition = "text")
    private String messageText;


    @Column(name = "intent")
    private String intent;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    public ChatMessage() {
    }


    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (role != null) {
            role = role.trim().toUpperCase();
        }
    }


    public Long getId() {
        return id;
    }


    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }


    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
