package com.example.backend.model;

import jakarta.persistence.*;

/**
 * 🗂️ مدل دسته‌بندی آگهی‌ها — قابل مدیریت توسط مدیر سیستم (افزودن/حذف)
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
