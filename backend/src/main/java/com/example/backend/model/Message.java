package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔄 تغییر اصلی: پیام به جای گیرنده، مستقیماً به یک گفتگو (Conversation) متصل می‌شود
    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    private String senderUsername;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;

    // سازنده‌ها
    public Message() {}

    public Message(Conversation conversation, String senderUsername, String content, LocalDateTime timestamp) {
        this.conversation = conversation;
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
    }

    // گترها و سترها
    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}