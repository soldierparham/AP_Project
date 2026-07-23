package com.example.backend.repository;

import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // 👈 اضافه شد
import org.springframework.data.jpa.repository.Query;    // 👈 اضافه شد
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // 👈 اضافه شد

import java.util.Optional;

/**
 * ریپازیتوری JPA کاربران؛ کوئری‌های یافتن کاربر بر اساس نام کاربری/شماره تلفن.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 🌟 پیدا کردن کاربر بر اساس نام کاربری (برای چک کردن سشن و خروج)
    /**
     * «by username» را جستجو و پیدا می‌کند.
     *
     * @param username نام کاربری
     * @return مقدار موردنظر در صورت وجود
     */
    Optional<User> findByUsername(String username);

    // 🌟 پیدا کردن کاربر بر اساس شماره تلفن (برای فرآیند لاگین)
    /**
     * «by phone number» را جستجو و پیدا می‌کند.
     *
     * @param phoneNumber پارامتر phoneNumber
     * @return مقدار موردنظر در صورت وجود
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    // بررسی تکراری بودن نام کاربی هنگام ثبت‌نام
    /**
     * بررسی وجود «by username».
     *
     * @param username نام کاربری
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByUsername(String username);

    // بررسی تکراری بودن شماره تلفن هنگام ثبت‌نام
    /**
     * بررسی وجود «by phone number».
     *
     * @param phoneNumber پارامتر phoneNumber
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByPhoneNumber(String phoneNumber);

    // بررسی تکراری بودن ایمیل هنگام ثبت‌نام
    /**
     * بررسی وجود «by email».
     *
     * @param email پارامتر email
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByEmail(String email);

    /**
     * 🧹 متد جدید: باطل کردن تمام توکن‌های دیتابیس SQLite به صورت یکجا
     * این متد توسط DatabaseInitializer در زمان بالا آمدن سرور صدا زده می‌شود.
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.jwtToken = null")
    void clearAllTokensOnStartup();
}