package com.example.backend.repository;

import com.example.backend.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ریپازیتوری JPA آگهی‌ها؛ متدهای CRUD و کوئری‌های سفارشی جدول آگهی‌ها.
 */
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    // آگهی‌های یک کاربر مشخص
    /**
     * «by owner username» را جستجو و پیدا می‌کند.
     *
     * @param ownerUsername پارامتر ownerUsername
     * @return لیست نتایج
     */
    List<Advertisement> findByOwnerUsername(String ownerUsername);

    // آگهی‌های همه کاربران به‌جز یک کاربر مشخص
    /**
     * «by owner username not» را جستجو و پیدا می‌کند.
     *
     * @param ownerUsername پارامتر ownerUsername
     * @return لیست نتایج
     */
    List<Advertisement> findByOwnerUsernameNot(String ownerUsername);

    // 🟢 جدید: آگهی‌ها بر اساس وضعیت (برای پنل مدیریت)
    /**
     * «by status» را جستجو و پیدا می‌کند.
     *
     * @param status پارامتر status
     * @return لیست نتایج
     */
    List<Advertisement> findByStatus(String status);

    // 🟢 جدید: آگهی‌های فعال دیگران (نمایش عمومی فقط آگهی‌های ACTIVE)
    /**
     * «by status and owner username not» را جستجو و پیدا می‌کند.
     *
     * @param status پارامتر status
     * @param ownerUsername پارامتر ownerUsername
     * @return لیست نتایج
     */
    List<Advertisement> findByStatusAndOwnerUsernameNot(String status, String ownerUsername);

    // 🟢 جدید: شمارش آگهی‌ها بر اساس وضعیت (برای داشبورد آماری مدیر)
    /**
     * تعداد «by status» را محاسبه می‌کند.
     *
     * @param status پارامتر status
     * @return مقدار عددی نتیجه
     */
    long countByStatus(String status);

    // ✏️ جدید: آگهی‌های یک دسته‌بندی مشخص (برای ویرایش نام دسته توسط ادمین)
    /**
     * «by category» را جستجو و پیدا می‌کند.
     *
     * @param category دسته‌بندی
     * @return لیست نتایج
     */
    List<Advertisement> findByCategory(String category);

    // 🏙️ جدید: آگهی‌های یک شهر مشخص (برای ویرایش نام شهر توسط ادمین)
    /**
     * «by city» را جستجو و پیدا می‌کند.
     *
     * @param city شهر
     * @return لیست نتایج
     */
    List<Advertisement> findByCity(String city);

    // ✏️ جدید: آگهی‌های یک دسته‌بندی بدون حساسیت به بزرگی/کوچکی حروف (برای تغییر نام دسته)
    /**
     * آگهی‌های یک دسته‌بندی را بدون حساسیت به بزرگی/کوچکی حروف پیدا می‌کند.
     *
     * @param category دسته‌بندی
     * @return لیست نتایج
     */
    List<Advertisement> findByCategoryIgnoreCase(String category);

    // 🏙️ جدید: آگهی‌های یک شهر بدون حساسیت به بزرگی/کوچکی حروف (برای تغییر نام شهر)
    /**
     * آگهی‌های یک شهر را بدون حساسیت به بزرگی/کوچکی حروف پیدا می‌کند.
     *
     * @param city شهر
     * @return لیست نتایج
     */
    List<Advertisement> findByCityIgnoreCase(String city);
}
