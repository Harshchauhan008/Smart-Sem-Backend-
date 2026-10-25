
package com.smartsem.smartsem.dto;

public class ChatResponse {

    private Long conversationId;
    private Long userMessageId;
    private String userMessage;
    private Long assistantMessageId;
    private String answer;
    private String intent;

    public ChatResponse() {
    }

    public ChatResponse(
            Long conversationId,
            Long userMessageId,
            String userMessage,
            Long assistantMessageId,
            String answer,
            String intent) {

        this.conversationId = conversationId;
        this.userMessageId = userMessageId;
        this.userMessage = userMessage;
        this.assistantMessageId = assistantMessageId;
        this.answer = answer;
        this.intent = intent;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getUserMessageId() {
        return userMessageId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Long getAssistantMessageId() {
        return assistantMessageId;
    }

    public String getAnswer() {
        return answer;
    }

    public String getIntent() {
        return intent;
    }
}
