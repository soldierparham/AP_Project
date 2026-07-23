package com.example.backend.dto;

/** پاسخ ورود موفق شامل توکن JWT */
public class AuthResponse {

    private boolean success;
    private String message;
    private String token;

    /**
     * سازنده کلاس AuthResponse؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public AuthResponse() {}

    /**
     * سازنده کلاس AuthResponse؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param success پارامتر success
     * @param message پیام
     * @param token توکن JWT
     */
    public AuthResponse(boolean success, String message, String token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    /**
     * بررسی می‌کند که آیا «success» برقرار است یا خیر.
     *
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    public boolean isSuccess() { return success; }
    /**
     * مقدار «success» را تنظیم می‌کند.
     *
     * @param success پارامتر success
     */
    public void setSuccess(boolean success) { this.success = success; }

    /**
     * مقدار «message» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getMessage() { return message; }
    /**
     * مقدار «message» را تنظیم می‌کند.
     *
     * @param message پیام
     */
    public void setMessage(String message) { this.message = message; }

    /**
     * مقدار «token» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getToken() { return token; }
    /**
     * مقدار «token» را تنظیم می‌کند.
     *
     * @param token توکن JWT
     */
    public void setToken(String token) { this.token = token; }
}
