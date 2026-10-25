
package com.smartsem.smartsem.dto;

public class ChatRequest {

    private String messageText;
    private String intent;

    public ChatRequest() {
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
}
