package com.example.backend.repository;

import com.example.backend.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // 1. لیست آگهی‌های اختصاصی هر کاربر (برای صفحه "آگهی‌های من")
    List<Advertisement> findByOwnerUsername(String ownerUsername);

    // 2. لیست تمام آگهی‌ها به جز مالِ خودِ کاربر (برای صفحه اصلی)
    List<Advertisement> findByOwnerUsernameNot(String ownerUsername);
}