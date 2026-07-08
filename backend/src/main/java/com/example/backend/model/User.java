package com.example.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // کاملاً هماهنگ با فرآیند AUTOINCREMENT در SQLite
    private Long id;

    @Column(nullable = false) // نام کاربر نباید تهی باشد
    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    // نام‌گذاری صریح ستون دیتابیس برای فیلدهای CamelCase
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // استانداردسازی مقادیر نقش و وضعیت به انگلیسی
    @Column(nullable = false)
    private String role = "USER";

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "jwt_token", length = 500)
    private String jwtToken;

    // سازنده پیش‌فرض (توسط JPA الزامی است)
    public User() {}

    // گترها و سترها (Getters & Setters)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJwtToken() { return jwtToken; }
    public void setJwtToken(String jwtToken) { this.jwtToken = jwtToken; }

    // =======================================================
    // 🛠️ متدهای پل‌بزن (سازگارکننده با فیلتر امنیت JwtFilter)
    // =======================================================

    /**
     * این متد همان مقدار jwtToken را برمی‌گرداند تا ارور کدهای امنیت برطرف شود.
     */
    public String getToken() {
        return this.jwtToken;
    }

    /**
     * این متد مقدار جدید را مستقیماً روی فیلد اصلی ست می‌کند.
     */
    public void setToken(String token) {
        this.jwtToken = token;
    }
}