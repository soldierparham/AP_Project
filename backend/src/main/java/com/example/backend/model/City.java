package com.example.backend.model;

import jakarta.persistence.*;

/**
 * 🏙️ مدل شهرها — قابل مدیریت توسط مدیر سیستم (افزودن/ویرایش/حذف)
 */
@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * سازنده کلاس City؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     */
    public City() {}

    /**
     * سازنده کلاس City؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param name نام
     */
    public City(String name) {
        this.name = name;
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
}
