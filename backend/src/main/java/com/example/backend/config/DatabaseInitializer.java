package com.example.backend.config;

import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    @Autowired
    private UserRepository userRepository;

    /**
     * 🧹 به محض بالا آمدن کامل بک‌انند، تمام توکن‌های دیتابیس SQLite پاک می‌شوند
     */
    @EventListener(ApplicationReadyEvent.class)
    public void clearTokensOnStartup() {
        try {
            userRepository.clearAllTokensOnStartup();
            System.out.println("🔄 دیتابیس ریست شد: تمام توکن‌های JWT قبلی باطل شدند.");
        } catch (Exception e) {
            System.err.println("❌ خطا در پاک‌سازی توکن‌های استارتاپ: " + e.getMessage());
        }
    }
}