package com.example.backend.controller;

import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import com.example.backend.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdvertisementController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository; // 💾 اضافه شد: برای به‌روزرسانی توکن جدید در دیتابیس

    /**
     * 📥 ۱. دریافت لیست آگهی‌ها (GET)
     */
    @GetMapping("/advertisements")
    public ResponseEntity<?> getAdvertisements(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ") ||
                authHeader.substring(7).trim().isEmpty() ||
                authHeader.substring(7).equalsIgnoreCase("null") ||
                authHeader.substring(7).equalsIgnoreCase("undefined")) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن یافت نشد یا معتبر نیست. لطفا وارد حساب خود شوید."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "توکن نامعتبر است."));
            }

            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما معتبر نیست."));
            }

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "اعتبار نشست شما باطل شده است."));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "لیست آگهی‌ها با موفقیت بارگذاری شد."
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
                System.out.println("🧹 [GET] توکن منقضی شده کاربر " + expiredUsername + " در دیتابیس null شد.");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره وارد شوید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "خطا در اعتبارسنجی توکن."));
        }
    }

    /**
     * 📤 ۲. ثبت آگهی جدید (POST) + تمدید خودکار زمان توکن
     */
    @PostMapping("/advertisements")
    public ResponseEntity<?> createAdvertisement(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> advertisementData,
            HttpServletResponse response) { // 🌐 اضافه شد: برای فرستادن هدرهای توکن جدید به فرانت‌انند

        // 🛡️ فیلتر دفاعی: جلوگیری از ورود هدرهای خالی، null یا undefined فرانت‌انند
        if (authHeader == null || !authHeader.startsWith("Bearer ") ||
                authHeader.substring(7).trim().isEmpty() ||
                authHeader.substring(7).equalsIgnoreCase("null") ||
                authHeader.substring(7).equalsIgnoreCase("undefined")) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن یافت نشد. لطفا برای ثبت آگهی وارد حساب خود شوید."));
        }

        String token = authHeader.substring(7);

        try {
            // استخراج و اعتبارسنجی نام کاربری
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "کاربر نامعتبر است."));
            }

            if (!jwtUtil.validateToken(token, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما معتبر نیست."));
            }

            // بررسی فیلد دیتابیس
            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "اعتبار نشست شما باطل شده است."));
            }

            // 🟢 [منطق اصلی ثبت آگهی شما]
            System.out.println("📝 آگهی جدید از کاربر " + username + " دریافت شد: " + advertisementData);

            // 🔄 ۳. تمدید زمان (Sliding Expiration): تولید توکن جدید و تازه‌نفس
            String newToken = jwtUtil.generateToken(username);

            // 💾 ۴. به‌روزرسانی فوری توکن جدید در دیتابیس SQLite
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setToken(newToken);
                userRepository.saveAndFlush(user);
                System.out.println("🔄 [POST] زمان توکن کاربر " + username + " هنگام ثبت آگهی تمدید و در دیتابیس آپدیت شد.");
            });

            // 🌐 ۵. تزریق توکن جدید به هدرهای پاسخ HTTP
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            // ارسال پاسخ موفقیت همراه با پیام و توکن جدید (جهت اطمینان بیشتر برای فرانت‌انند)
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی شما با موفقیت ثبت شد.",
                    "token", newToken // فرانت‌انند هم از طریق بدنه و هم هدر به توکن جدید دسترسی دارد
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
                System.out.println("🧹 [POST] توکن کاربر " + expiredUsername + " موقع ثبت آگهی منقضی شد و در دیتابیس null گردید.");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما به پایان رسیده است. لطفاً دوباره وارد شوید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "خطا در پردازش درخواست ثبت آگهی."));
        }
    }
}