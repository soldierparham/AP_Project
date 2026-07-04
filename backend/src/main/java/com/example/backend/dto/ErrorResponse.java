package com.example.backend.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    private String code;          // کد فنی خطا برای فرانت‌انند (مثلاً VALIDATION_ERROR)
    private String message;       // پیام فارسی و قابل فهم برای نمایش به کاربر
    private LocalDateTime timestamp; // زمان دقیق رخ دادن خطا

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    // Getters & Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}