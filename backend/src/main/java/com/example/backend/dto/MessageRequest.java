package com.example.backend.dto;

public class MessageRequest {
    private Long conversationId;
    private String content;

    // سازنده‌ها
    public MessageRequest() {}

    public MessageRequest(Long conversationId, String content) {
        this.conversationId = conversationId;
        this.content = content;
    }

    // گترها و سترها
    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}