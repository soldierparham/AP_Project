package com.example.backend.repository;

import com.example.backend.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // آگهی‌های یک کاربر مشخص
    List<Advertisement> findByOwnerUsername(String ownerUsername);

    // آگهی‌های همه کاربران به‌جز یک کاربر مشخص
    List<Advertisement> findByOwnerUsernameNot(String ownerUsername);

    // 🟢 جدید: آگهی‌ها بر اساس وضعیت (برای پنل مدیریت)
    List<Advertisement> findByStatus(String status);

    // 🟢 جدید: آگهی‌های فعال دیگران (نمایش عمومی فقط آگهی‌های ACTIVE)
    List<Advertisement> findByStatusAndOwnerUsernameNot(String status, String ownerUsername);

    // 🟢 جدید: شمارش آگهی‌ها بر اساس وضعیت (برای داشبورد آماری مدیر)
    long countByStatus(String status);

    // ✏️ جدید: آگهی‌های یک دسته‌بندی مشخص (برای ویرایش نام دسته توسط ادمین)
    List<Advertisement> findByCategory(String category);

    // 🏙️ جدید: آگهی‌های یک شهر مشخص (برای ویرایش نام شهر توسط ادمین)
    List<Advertisement> findByCity(String city);
}
