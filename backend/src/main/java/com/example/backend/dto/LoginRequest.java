package com.example.backend.dto;

public class LoginRequest {
    private String username; // حاوی شماره تماس کاربر برای لاگین
    private String password;

    // Getters & Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}