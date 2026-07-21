package com.example.backend.model;

import jakarta.persistence.*;

/**
 * مدل دسته‌بندی آگهی‌ها — قابل مدیریت توسط مدیر سیستم (افزودن/ویرایش/حذف)
 * جدید: پشتیبانی از زیردسته‌بندی با فیلد parentId (null = دسته اصلی)
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    /** شناسه دسته والد — null یعنی دسته اصلی (سطح اول) */
    @Column(name = "parent_id")
    private Long parentId;

    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Category(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
