package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ⭐ مدل علاقه‌مندی (نشان کردن آگهی) — هر کاربر هر آگهی را فقط یک بار می‌تواند نشان کند.
 */
@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "advertisement_id"})
)
/**
 * موجودیت (Entity) علاقه‌مندی؛ نگاشت جدول علاقه‌مندی کاربران به آگهی‌ها.
 */
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @ManyToOne(optional = false)
    @JoinColumn(name = "advertisement_id")
    private Advertisement advertisement;

    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * سازنده کلاس Favorite؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Favorite() {}

    /**
     * سازنده کلاس Favorite؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param username نام کاربری
     * @param advertisement پارامتر advertisement
     */
    public Favorite(String username, Advertisement advertisement) {
        this.username = username;
        this.advertisement = advertisement;
        this.createdAt = LocalDateTime.now();
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
     * مقدار «advertisement» را برمی‌گرداند.
     *
     * @return شیء آگهی
     */
    public Advertisement getAdvertisement() { return advertisement; }
    /**
     * مقدار «advertisement» را تنظیم می‌کند.
     *
     * @param advertisement پارامتر advertisement
     */
    public void setAdvertisement(Advertisement advertisement) { this.advertisement = advertisement; }

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
