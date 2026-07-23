package com.example.backend.dto;

/** بدنه درخواست ورود (فیلد username حاوی شماره تماس است) */
public class LoginRequest {

    private String username;
    private String password;

    /**
     * سازنده کلاس LoginRequest؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public LoginRequest() {}

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
}
