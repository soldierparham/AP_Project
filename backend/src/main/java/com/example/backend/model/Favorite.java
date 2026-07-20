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

    public Favorite() {}

    public Favorite(String username, Advertisement advertisement) {
        this.username = username;
        this.advertisement = advertisement;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Advertisement getAdvertisement() { return advertisement; }
    public void setAdvertisement(Advertisement advertisement) { this.advertisement = advertisement; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
