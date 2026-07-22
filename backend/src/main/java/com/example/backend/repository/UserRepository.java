package com.example.backend.repository;

import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying; // 👈 اضافه شد
import org.springframework.data.jpa.repository.Query;    // 👈 اضافه شد
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // 👈 اضافه شد

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 🌟 پیدا کردن کاربر بر اساس نام کاربری (برای چک کردن سشن و خروج)
    Optional<User> findByUsername(String username);

    // 🌟 پیدا کردن کاربر بر اساس شماره تلفن (برای فرآیند لاگین)
    Optional<User> findByPhoneNumber(String phoneNumber);

    // بررسی تکراری بودن نام کاربی هنگام ثبت‌نام
    boolean existsByUsername(String username);

    // بررسی تکراری بودن شماره تلفن هنگام ثبت‌نام
    boolean existsByPhoneNumber(String phoneNumber);

    // بررسی تکراری بودن ایمیل هنگام ثبت‌نام
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