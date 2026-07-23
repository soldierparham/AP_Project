package com.example.backend.model;

import jakarta.persistence.*;

/**
 * موجودیت (Entity) مکالمه؛ نگاشت جدول گفتگو بین دو کاربر درباره یک آگهی.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔧 nullable=true: اگر آگهی حذف شود، مکالمه باقی می‌ماند و آگهی null می‌شود
    // (در عمل deleteAd() مکالمات را هم حذف می‌کند، این فقط failsafe است)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "advertisement_id", nullable = true)
    private Advertisement advertisement;

    private String buyerUsername;
    private String sellerUsername;

    // ساده‌سازی: عنوان آگهی را در خود Conversation ذخیره می‌کنیم
    // تا حتی اگر آگهی حذف شود، تاریخچه چت قابل مشاهده باشد
    @Column(name = "ad_title_snapshot")
    private String adTitleSnapshot;

    /**
     * سازنده کلاس Conversation؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Conversation() {}

    /**
     * سازنده کلاس Conversation؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param advertisement پارامتر advertisement
     * @param buyerUsername پارامتر buyerUsername
     * @param sellerUsername نام کاربری فروشنده
     */
    public Conversation(Advertisement advertisement, String buyerUsername, String sellerUsername) {
        this.advertisement = advertisement;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
        // عنوان آگهی را در همان لحظه ایجاد ذخیره می‌کنیم
        this.adTitleSnapshot = advertisement != null ? advertisement.getTitle() : "";
    }

    // --- گترها و سترها ---
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
     * مقدار «buyer username» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getBuyerUsername() { return buyerUsername; }
    /**
     * مقدار «buyer username» را تنظیم می‌کند.
     *
     * @param buyerUsername پارامتر buyerUsername
     */
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }
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
     * مقدار «ad title snapshot» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getAdTitleSnapshot() { return adTitleSnapshot; }
    /**
     * مقدار «ad title snapshot» را تنظیم می‌کند.
     *
     * @param adTitleSnapshot پارامتر adTitleSnapshot
     */
    public void setAdTitleSnapshot(String adTitleSnapshot) { this.adTitleSnapshot = adTitleSnapshot; }
}
