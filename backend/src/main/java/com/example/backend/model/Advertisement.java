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

    public Advertisement() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
