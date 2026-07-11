package com.example.backend.dto;

public class MessageRequest {
    private String receiverUsername;
    private String content;

    // Getters and Setters
    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String receiverUsername) { this.receiverUsername = receiverUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}