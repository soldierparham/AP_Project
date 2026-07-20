package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import com.example.backend.service.AdvertisementService;
import com.example.backend.service.AuthService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdvertisementController {

    @Autowired
    private AdvertisementService advertisementService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 🔍 دریافت آگهی‌های فعال دیگران (صفحه اصلی) با جستجو، فیلتر ترکیبی و مرتب‌سازی
     * پارامترهای اختیاری: search, category, city, minPrice, maxPrice, sort(newest|cheapest|expensive)
     */
    @GetMapping("/advertisements")
    public ResponseEntity<?> getAllAds(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            // 🟢 فقط آگهی‌های ACTIVE (تاییدشده توسط مدیر) برای عموم نمایش داده می‌شوند
            List<Advertisement> ads = advertisementService.getActiveAdsExceptOwner(
                    username, search, category, city, minPrice, maxPrice, sort);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "لیست آگهی‌ها با موفقیت دریافت شد.",
                    "data", ads
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن نامعتبر است."));
        }
    }

    /**
     * 👤 دریافت آگهی‌های خود کاربر (با هر وضعیتی: در انتظار تایید، فعال، رد شده، فروخته شده)
     */
    @GetMapping("/advertisements/my")
    public ResponseEntity<?> getMyAds(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            // 🔍 فیلترهای ردیف بالا روی آگهی‌های خود کاربر هم اعمال می‌شود
            List<Advertisement> myAds = advertisementService.getAdsByUsername(
                    username, search, category, city, minPrice, maxPrice, sort);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی‌های شما با موفقیت دریافت شد.",
                    "data", myAds
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن نامعتبر است."));
        }
    }

    /**
     * 📥 ثبت آگهی جدید (با وضعیت اولیه «در انتظار تایید») + چرخش توکن
     */
    @PostMapping("/advertisements")
    public ResponseEntity<?> createAd(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> adData,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            // ✅ اعتبارسنجی ورودی‌ها در سمت سرور
            if (adData.get("title") == null || adData.get("title").toString().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "عنوان آگهی الزامی است."));
            }
            if (adData.get("description") == null || adData.get("description").toString().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "توضیحات آگهی الزامی است."));
            }
            if (adData.get("price") == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "قیمت آگهی الزامی است."));
            }
            long priceValue;
            try {
                priceValue = Long.parseLong(adData.get("price").toString().replaceAll("\\..*", ""));
            } catch (NumberFormatException nfe) {
                return ResponseEntity.badRequest().body(Map.of("message", "قیمت باید یک عدد معتبر باشد."));
            }
            if (priceValue < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "قیمت نمی‌تواند منفی باشد."));
            }

            Advertisement savedAd = advertisementService.saveAdvertisement(adData, username);

            // 🔄 چرخش توکن (با حفظ نقش کاربر در توکن جدید)
            String newToken = rotateToken(username);
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "آگهی شما ثبت شد و پس از تایید مدیر نمایش داده خواهد شد.",
                    "data", savedAd,
                    "token", newToken
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در ثبت آگهی: " + e.getMessage()));
        }
    }

    /**
     * ✏️ ویرایش آگهی (فقط توسط مالک) — پس از ویرایش به صف بررسی مجدد می‌رود
     */
    @PutMapping("/advertisements/{id}")
    public ResponseEntity<?> updateAd(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> adData,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            Optional<Advertisement> adOpt = advertisementService.getAdById(id);
            if (adOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
            }

            Advertisement existingAd = adOpt.get();
            if (!existingAd.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "شما فقط مجاز به ویرایش آگهی‌های خودتان هستید."));
            }

            Advertisement updatedAd = advertisementService.updateAdvertisement(existingAd, adData);

            String newToken = rotateToken(username);
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی ویرایش شد و برای بازبینی مجدد به مدیر ارسال گردید.",
                    "data", updatedAd,
                    "token", newToken
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در ویرایش آگهی: " + e.getMessage()));
        }
    }

    /**
     * ✅ جدید: علامت‌گذاری آگهی به عنوان «فروخته‌شده» (فقط توسط مالک آگهی)
     */
    @PostMapping("/advertisements/{id}/sold")
    public ResponseEntity<?> markAdAsSold(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            Optional<Advertisement> adOpt = advertisementService.getAdById(id);
            if (adOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
            }

            Advertisement ad = adOpt.get();
            if (!ad.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "فقط مالک آگهی می‌تواند آن را فروخته‌شده اعلام کند."));
            }

            Advertisement soldAd = advertisementService.markAsSold(ad);

            String newToken = rotateToken(username);
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی با موفقیت فروخته‌شده اعلام شد.",
                    "data", soldAd,
                    "token", newToken
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در به‌روزرسانی آگهی: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ حذف آگهی (فقط توسط مالک)
     */
    @DeleteMapping("/advertisements/{id}")
    public ResponseEntity<?> deleteAd(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن احراز هویت یافت نشد."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (!authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
            }

            Optional<Advertisement> adOpt = advertisementService.getAdById(id);
            if (adOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
            }

            Advertisement ad = adOpt.get();
            if (!ad.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "شما فقط مجاز به حذف آگهی‌های خودتان هستید."));
            }

            advertisementService.deleteAd(id);

            String newToken = rotateToken(username);
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی با موفقیت حذف شد.",
                    "token", newToken
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) {
                authService.logout(expiredUsername);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "نشست شما منقضی شده است. لطفاً دوباره ورود کنید."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در حذف آگهی: " + e.getMessage()));
        }
    }

    /**
     * 🔄 چرخش توکن پس از عملیات نوشتنی — نقش کاربر در توکن جدید حفظ می‌شود
     * (در نسخه قبلی توکن جدید بدون نقش ساخته می‌شد و نقش ADMIN از دست می‌رفت)
     */
    private String rotateToken(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        String newToken = userOpt.isPresent()
                ? jwtUtil.generateToken(username, userOpt.get().getRole())
                : jwtUtil.generateToken(username, "USER");
        userOpt.ifPresent(user -> {
            user.setToken(newToken);
            userRepository.saveAndFlush(user);
        });
        return newToken;
    }
}
