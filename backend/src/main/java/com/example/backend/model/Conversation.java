package com.example.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;

    private String buyerUsername;
    private String sellerUsername;

    // ۱. سازنده بدون آرگومان (برای استفاده JPA/Hibernate الزامی است)
    public Conversation() {}

    // ۲. سازنده ۳ آرگومانه (این بخش را حتماً اضافه کنید تا خطا برطرف شود)
    public Conversation(Advertisement advertisement, String buyerUsername, String sellerUsername) {
        this.advertisement = advertisement;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
    }

    // --- بقیه گترها و سترها ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Advertisement getAdvertisement() { return advertisement; }
    public void setAdvertisement(Advertisement advertisement) { this.advertisement = advertisement; }
    public String getBuyerUsername() { return buyerUsername; }
    public void setBuyerUsername(String buyerUsername) { this.buyerUsername = buyerUsername; }
    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
}