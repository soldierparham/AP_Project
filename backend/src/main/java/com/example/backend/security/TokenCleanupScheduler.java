package com.example.backend.security;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenCleanupScheduler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * ⏳ این متد هر ۶۰ ثانیه یک‌بار (60000 میلی‌ثانیه) در پس‌زمینه اجرا می‌شود.
     * می‌توانید زمان آن را بر اساس نیاز خود تغییر دهید.
     */
    @Scheduled(fixedRate = 60000)
    public void removeExpiredTokensFromDatabase() {
        System.out.println("🔄 [Scheduler] در حال بررسی و پاک‌سازی توکن‌های منقضی شده در دیتابیس...");

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if (user.getToken() != null) {
                try {
                    // تلاش برای اعتبارسنجی توکن؛ اگر منقضی شده باشد خطای ExpiredJwtException پرتاب می‌شود
                    if (jwtUtil.isTokenExpired(user.getToken())) {
                        nullifyUserToken(user);
                    }
                } catch (ExpiredJwtException e) {
                    // 🎯 توکن قطعاً منقضی شده است
                    nullifyUserToken(user);
                } catch (Exception e) {
                    // اگر توکن به هر دلیل دیگری ساختار خرابی داشت، آن را هم پاک می‌کنیم
                    nullifyUserToken(user);
                }
            }
        }
    }

    /**
     * متد کمکی برای نال کردن امن توکن در SQLite
     */
    private void nullifyUserToken(User user) {
        try {
            String username = user.getUsername();
            user.setToken(null); // نال کردن توکن در دیتابیس
            userRepository.saveAndFlush(user); // اعمال آنی
            System.out.println("🧼 [Scheduler] توکن منقضی شده کاربر " + username + " به صورت خودکار نال شد.");
        } catch (Exception e) {
            System.out.println("❌ [Scheduler] خطا در به‌روزرسانی دیتابیس: " + e.getMessage());
        }
    }
}