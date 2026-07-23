package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 🏷️ مدل آگهی — نسخه تکمیل‌شده مطابق سند پروژه:
 * فیلدهای جدید: status (وضعیت آگهی)، adminNote (یادداشت مدیر هنگام رد)، createdAt (زمان ثبت)
 * وضعیت‌های مجاز: PENDING (در انتظار تایید)، ACTIVE (فعال)، REJECTED (رد شده)، SOLD (فروخته شده)
 */
@Entity
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private Long price;

    private String city;

    private String category;

    private String ownerUsername;

    @Column(length = 2000)
    private String imageUrl;

    // 🟢 وضعیت آگهی: هر آگهی جدید ابتدا در انتظار تایید مدیر است
    private String status = "PENDING";

    // 📝 یادداشت مدیر (مثلاً دلیل رد شدن آگهی)
    @Column(length = 1000)
    private String adminNote;

    // ⏱️ زمان ثبت آگهی (برای مرتب‌سازی «جدیدترین»)
    private LocalDateTime createdAt;

    /**
     * سازنده کلاس Advertisement؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Advertisement() {}

    /**
     * متد «prePersist»؛ بخشی از عملکرد کلاس Advertisement را پیاده‌سازی می‌کند.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

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
     * مقدار «title» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getTitle() { return title; }
    /**
     * مقدار «title» را تنظیم می‌کند.
     *
     * @param title عنوان
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * مقدار «description» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getDescription() { return description; }
    /**
     * مقدار «description» را تنظیم می‌کند.
     *
     * @param description توضیحات
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * مقدار «price» را برمی‌گرداند.
     *
     * @return مقدار عددی نتیجه
     */
    public Long getPrice() { return price; }
    /**
     * مقدار «price» را تنظیم می‌کند.
     *
     * @param price قیمت
     */
    public void setPrice(Long price) { this.price = price; }

    /**
     * مقدار «city» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getCity() { return city; }
    /**
     * مقدار «city» را تنظیم می‌کند.
     *
     * @param city شهر
     */
    public void setCity(String city) { this.city = city; }

    /**
     * مقدار «category» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getCategory() { return category; }
    /**
     * مقدار «category» را تنظیم می‌کند.
     *
     * @param category دسته‌بندی
     */
    public void setCategory(String category) { this.category = category; }

    /**
     * مقدار «owner username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getOwnerUsername() { return ownerUsername; }
    /**
     * مقدار «owner username» را تنظیم می‌کند.
     *
     * @param ownerUsername پارامتر ownerUsername
     */
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    /**
     * مقدار «image url» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getImageUrl() { return imageUrl; }
    /**
     * مقدار «image url» را تنظیم می‌کند.
     *
     * @param imageUrl آدرس تصویر
     */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

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
     * مقدار «admin note» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getAdminNote() { return adminNote; }
    /**
     * مقدار «admin note» را تنظیم می‌کند.
     *
     * @param adminNote پارامتر adminNote
     */
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    /**
     * مقدار «created at» را برمی‌گرداند.
     *
     * @return مقدار بازگشتی
     */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /**
     * مقدار «created at» را تنظیم می‌کند.
     *
     * @param createdAt تاریخ ایجاد
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
