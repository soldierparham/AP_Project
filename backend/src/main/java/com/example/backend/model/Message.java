package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** پیام متنی داخل یک گفتگو */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * سازنده کلاس Message؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Message() {}

    /**
     * مقدار «id» را برمی‌گرداند.
     *
     * @return مقدار عددی نتیجه
     */
    public Long getId() { return id; }
    /**
     * مقدار «id» را تنظیم می‌کند.
     *
     * @param id شناسه
     */
    public void setId(Long id) { this.id = id; }

    /**
     * مقدار «conversation» را برمی‌گرداند.
     *
     * @return مقدار بازگشتی
     */
    public Conversation getConversation() { return conversation; }
    /**
     * مقدار «conversation» را تنظیم می‌کند.
     *
     * @param conversation پارامتر conversation
     */
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    /**
     * مقدار «sender username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getSenderUsername() { return senderUsername; }
    /**
     * مقدار «sender username» را تنظیم می‌کند.
     *
     * @param senderUsername پارامتر senderUsername
     */
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    /**
     * مقدار «content» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getContent() { return content; }
    /**
     * مقدار «content» را تنظیم می‌کند.
     *
     * @param content پارامتر content
     */
    public void setContent(String content) { this.content = content; }

    /**
     * مقدار «timestamp» را برمی‌گرداند.
     *
     * @return مقدار بازگشتی
     */
    public LocalDateTime getTimestamp() { return timestamp; }
    /**
     * مقدار «timestamp» را تنظیم می‌کند.
     *
     * @param timestamp پارامتر timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
