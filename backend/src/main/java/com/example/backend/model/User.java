package com.example.backend.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

/**
 * موجودیت (Entity) کاربر؛ نگاشت جدول کاربران شامل نام کاربری، رمز عبور رمزنگاری‌شده، نقش و وضعیت مسدودی.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

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
    /**
     * سازنده کلاس User؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public User() {}

    // گترها و سترها (Getters & Setters)
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
     * مقدار «name» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getName() { return name; }
    /**
     * مقدار «name» را تنظیم می‌کند.
     *
     * @param name نام
     */
    public void setName(String name) { this.name = name; }

    /**
     * مقدار «username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getUsername() { return username; }
    /**
     * مقدار «username» را تنظیم می‌کند.
     *
     * @param username نام کاربری
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * مقدار «phone number» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getPhoneNumber() { return phoneNumber; }
    /**
     * مقدار «phone number» را تنظیم می‌کند.
     *
     * @param phoneNumber پارامتر phoneNumber
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * مقدار «email» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getEmail() { return email; }
    /**
     * مقدار «email» را تنظیم می‌کند.
     *
     * @param email پارامتر email
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * مقدار «password» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getPassword() { return password; }
    /**
     * مقدار «password» را تنظیم می‌کند.
     *
     * @param password رمز عبور
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * مقدار «role» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getRole() { return role; }
    /**
     * مقدار «role» را تنظیم می‌کند.
     *
     * @param role پارامتر role
     */
    public void setRole(String role) { this.role = role; }

    /**
     * مقدار «status» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getStatus() { return status; }
    /**
     * مقدار «status» را تنظیم می‌کند.
     *
     * @param status پارامتر status
     */
    public void setStatus(String status) { this.status = status; }

    /**
     * مقدار «jwt token» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getJwtToken() { return jwtToken; }
    /**
     * مقدار «jwt token» را تنظیم می‌کند.
     *
     * @param jwtToken پارامتر jwtToken
     */
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

    /**
     * مقدار «authorities» را برمی‌گرداند.
     *
     * @return مقدار بازگشتی
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // یا لیستی از نقش‌ها را برگردانید
    }

    /**
     * بررسی می‌کند که آیا «account non expired» برقرار است یا خیر.
     *
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    @Override
    public boolean isAccountNonExpired() { return true; }
    /**
     * بررسی می‌کند که آیا «account non locked» برقرار است یا خیر.
     *
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    @Override
    public boolean isAccountNonLocked() { return true; }
    /**
     * بررسی می‌کند که آیا «credentials non expired» برقرار است یا خیر.
     *
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    /**
     * بررسی می‌کند که آیا «enabled» برقرار است یا خیر.
     *
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    @Override
    public boolean isEnabled() { return true; }

    // متد toString را هم اضافه کنید تا لاگ‌ها تمیز باشند
    /**
     * تبدیل به «string».
     *
     * @return رشته نتیجه
     */
    @Override
    public String toString() {
        return this.username;
    }
}