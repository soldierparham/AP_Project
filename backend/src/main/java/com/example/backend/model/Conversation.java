package com.example.backend.model;

import jakarta.persistence.*;

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

    public Conversation() {}

    public Conversation(Advertisement advertisement, String buyerUsername, String sellerUsername) {
        this.advertisement = advertisement;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
        // عنوان آگهی را در همان لحظه ایجاد ذخیره می‌کنیم
        this.adTitleSnapshot = advertisement != null ? advertisement.getTitle() : "";
    }

    // --- گترها و سترها ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Advertisement getAdvertisement() { return advertisement; }
    public void setAdvertisement(Advertisement advertisement) { this.advertisement = advertisement; }
    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }
    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
    public String getAdTitleSnapshot() { return adTitleSnapshot; }
    public void setAdTitleSnapshot(String adTitleSnapshot) { this.adTitleSnapshot = adTitleSnapshot; }
}
