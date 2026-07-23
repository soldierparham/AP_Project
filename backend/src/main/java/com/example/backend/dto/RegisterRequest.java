package com.example.backend.dto;

/** بدنه درخواست ثبت‌نام کاربر جدید */
public class RegisterRequest {

    private String name;
    private String username;
    private String phoneNumber;
    private String email;
    private String password;

    /**
     * سازنده کلاس RegisterRequest؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public RegisterRequest() {}

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
}
