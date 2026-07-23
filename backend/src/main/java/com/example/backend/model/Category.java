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

    /**
     * سازنده کلاس Category؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public Category() {}

    /**
     * سازنده کلاس Category؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param name نام
     */
    public Category(String name) {
        this.name = name;
    }

    /**
     * سازنده کلاس Category؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param name نام
     * @param parentId پارامتر parentId
     */
    public Category(String name, Long parentId) {
        this.name = name;
        this.parentId = parentId;
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
     * مقدار «name» را برمی‌گرداند.
     *
     * @return رشته نتیجه
     */
    public String getName() { return name; }
    /**
     * مقدار «name» را تنظیم می‌کند.
     *
     * @param name نام
     */
    public void setName(String name) { this.name = name; }

    /**
     * مقدار «parent id» را برمی‌گرداند.
     *
     * @return مقدار عددی نتیجه
     */
    public Long getParentId() { return parentId; }
    /**
     * مقدار «parent id» را تنظیم می‌کند.
     *
     * @param parentId پارامتر parentId
     */
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
