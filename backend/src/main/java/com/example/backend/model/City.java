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

    public City() {}

    public City(String name) {
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
