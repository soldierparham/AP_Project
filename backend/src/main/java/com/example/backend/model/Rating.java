package com.example.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ⭐ مدل امتیازدهی خریدار به فروشنده (۱ تا ۵)
 * - هر خریدار برای هر آگهی فقط یک بار می‌تواند امتیاز ثبت کند (قید یکتایی)
 * - امتیاز به خود (Self-rating) در کنترلر جلوگیری می‌شود
 */
@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rater_username", "advertisement_id"})
)
/**
 * موجودیت (Entity) امتیاز؛ نگاشت جدول امتیازها و نظرهای ثبت‌شده برای فروشندگان.
 */
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rater_username", nullable = false)
    private String raterUsername;

    @Column(name = "seller_username", nullable = false)
    private String sellerUsername;

    @ManyToOne(optional = false)
    @JoinColumn(name = "advertisement_id")
    private Advertisement advertisement;

    // امتیاز بین ۱ تا ۵
    @Column(nullable = false)
    private int score;

    // 💬 نظر اختیاری خریدار درباره فروشنده (می‌تواند خالی باشد)
    @Column(length = 1000)
    private String comment;

    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * سازنده کلاس Rating؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Rating() {}

    /**
     * سازنده کلاس Rating؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param raterUsername پارامتر raterUsername
     * @param sellerUsername نام کاربری فروشنده
     * @param advertisement پارامتر advertisement
     * @param score امتیاز (۱ تا ۵)
     */
    public Rating(String raterUsername, String sellerUsername, Advertisement advertisement, int score) {
        this.raterUsername = raterUsername;
        this.sellerUsername = sellerUsername;
        this.advertisement = advertisement;
        this.score = score;
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
     * مقدار «rater username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getRaterUsername() { return raterUsername; }
    /**
     * مقدار «rater username» را تنظیم می‌کند.
     *
     * @param raterUsername پارامتر raterUsername
     */
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }

    /**
     * مقدار «seller username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getSellerUsername() { return sellerUsername; }
    /**
     * مقدار «seller username» را تنظیم می‌کند.
     *
     * @param sellerUsername نام کاربری فروشنده
     */
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

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
     * مقدار «score» را برمی‌گرداند.
     *
     * @return مقدار عددی نتیجه
     */
    public int getScore() { return score; }
    /**
     * مقدار «score» را تنظیم می‌کند.
     *
     * @param score امتیاز (۱ تا ۵)
     */
    public void setScore(int score) { this.score = score; }

    /**
     * مقدار «comment» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getComment() { return comment; }
    /**
     * مقدار «comment» را تنظیم می‌کند.
     *
     * @param comment متن نظر
     */
    public void setComment(String comment) { this.comment = comment; }

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
