package com.example.backend.repository;

import com.example.backend.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // 🔍 کوئری اختصاصی برای پیدا کردن آگهی‌های یک کاربر خاص
    List<Advertisement> findByOwnerUsername(String ownerUsername);
}