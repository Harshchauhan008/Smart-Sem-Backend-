
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "semester_id")
    private Long semesterId;


    @Column(name = "title")
    private String title;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    public ChatConversation() {
    }


    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }


    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }


    public Long getId() {
        return id;
    }


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    }
}
