package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // جلوگیری از خطای CORS هنگام اتصال فرانت به بک
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * مدیریت درخواست ثبت‌نام
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            authService.registerNewUser(request);
            // بازگرداندن پاسخ موفقیت در قالب استاندارد JSON
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(true, "ثبت‌نام با موفقیت انجام شد.", null));
        } catch (IllegalArgumentException e) {
            // در صورت تکراری بودن نام کاربری یا شماره تلفن، ارور 400 برمی‌گردد
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * مدیریت درخواست ورود (لاگین)
     * 🌟 اصلاح شد: هماهنگی کامل با فرانت‌انند برای ارسال وضعیت 401 و پیام فارسی
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request) {
        try {
            // لایه سرویس در صورت موفقیت توکن می‌دهد و در صورت خطا Exception پرتاب می‌کند
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 🛡️ صید خطای مشخصات اشتباه و ارسال کد وضعیت 401 (Unauthorized)
            // استفاده از Map.of تضمین می‌کند کلید جی‌سان دقیقاً "message" باشد تا متد extractMessageFromJson در فرانت آن را بخواند.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 🔒 متد جدید: مدیریت درخواست خروج (لاگ‌اوت)
     * این اندپوینت توسط کلاس HelloController در فرانت‌انند فراخوانی می‌شود.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logoutUser(@RequestParam String username) {
        try {
            authService.logout(username);
            return ResponseEntity.ok(Map.of("message", "خروج با موفقیت انجام شد و توکن باطل گردید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در فرآیند خروج از سیستم."));
        }
    }
}