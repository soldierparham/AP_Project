package com.example.backend.config;

import com.example.backend.model.Category;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 🌱 ساخت داده‌های اولیه هنگام اجرای برنامه:
 * - حساب مدیر پیش‌فرض (شماره: 09000000000 ، رمز: admin123)
 * - دسته‌بندی‌های پیش‌فرض آگهی‌ها
 */
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedCategories();
    }

    private void seedAdminUser() {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> "ADMIN".equalsIgnoreCase(u.getRole()));

        if (!adminExists
                && !userRepository.existsByUsername("admin")
                && !userRepository.existsByPhoneNumber("09000000000")) {

            User admin = new User();
            admin.setName("مدیر سیستم");
            admin.setUsername("admin");
            admin.setPhoneNumber("09000000000");
            admin.setEmail("admin@secondhand.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");

            userRepository.saveAndFlush(admin);
            System.out.println("👑 حساب مدیر پیش‌فرض ساخته شد. (شماره: 09000000000 / رمز: admin123)");
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            List<String> defaults = List.of(
                    "کالای دیجیتال",
                    "وسایل نقلیه",
                    "املاک",
                    "خانه و آشپزخانه",
                    "وسایل شخصی",
                    "سرگرمی و فراغت",
                    "خدمات",
                    "تجهیزات و صنعتی"
            );
            defaults.forEach(name -> categoryRepository.save(new Category(name)));
            System.out.println("🗂️ دسته‌بندی‌های پیش‌فرض ساخته شدند.");
        }
    }
}
