package com.example.backend.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String token; // توکن JWT در صورت لاگین موفق اینجا قرار می‌گیرد

    // سازنده پیش‌فرض
    public AuthResponse() {}

    // سازنده کامل
    public AuthResponse(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    // گترها و سترها
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}