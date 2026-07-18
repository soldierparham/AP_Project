package com.example.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "advertisements")
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Double price;

    // 🏙️ فیلدهای جدید برای نگهداری موقعیت مکانی و صنف کالا
    private String city;
    private String category;

    // 🔑 ذخیره نام کاربری مالک آگهی برای بررسی‌های امنیتی CRUD
    private String ownerUsername;

    private String imageUrl;

    // گترها و سترها (Getters and Setters)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    // 🟢 گتر و ستر برای شهر
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    // 🟢 گتر و ستر برای دسته‌بندی
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}