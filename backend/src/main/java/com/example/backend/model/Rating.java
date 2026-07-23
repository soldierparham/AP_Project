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

    public Rating() {}

    public Rating(String raterUsername, String sellerUsername, Advertisement advertisement, int score) {
        this.raterUsername = raterUsername;
        this.sellerUsername = sellerUsername;
        this.advertisement = advertisement;
        this.score = score;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRaterUsername() { return raterUsername; }
    public void setRaterUsername(String raterUsername) { this.raterUsername = raterUsername; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }

    public Advertisement getAdvertisement() { return advertisement; }
    public void setAdvertisement(Advertisement advertisement) { this.advertisement = advertisement; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
