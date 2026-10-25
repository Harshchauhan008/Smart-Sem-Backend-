
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "ai_action_logs")
public class AiActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(name = "conversation_id")
    private Long conversationId;


    @Column(name = "action_type", nullable = false)
    private String actionType;


    @Column(name = "input_summary", columnDefinition = "text")
    private String inputSummary;


    @Column(name = "result_summary", columnDefinition = "text")
    private String resultSummary;


    @Column(nullable = false)
    private String status = "PENDING";


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    public AiActionLog() {
    }


    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
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


    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }


    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }


    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }


    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
