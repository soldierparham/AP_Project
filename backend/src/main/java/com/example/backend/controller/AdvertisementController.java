package com.example.backend.controller;

import com.example.backend.model.Advertisement;
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
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository; // 💾 برای به‌روزرسانی توکن جدید در دیتابیس SQLite

    @Autowired
    private AdvertisementService advertisementService;

    /**
     * 📥 ۱. دریافت تمام آگهی‌های موجود در سیستم (GET)
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
            if (username == null || !jwtUtil.validateToken(token, username) || !authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما معتبر نیست."));
            }

            // 🟢 دریافت واقعی داده‌ها از دیتابیس
            List<Advertisement> allAds = advertisementService.getAllAdvertisements();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "لیست آگهی‌ها با موفقیت بارگذاری شد.",
                    "data", allAds
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
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ") ||
                authHeader.substring(7).trim().isEmpty() ||
                authHeader.substring(7).equalsIgnoreCase("null") ||
                authHeader.substring(7).equalsIgnoreCase("undefined")) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "توکن یافت نشد. لطفا برای ثبت آگهی وارد حساب خود شوید."));
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token, username) || !authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما معتبر نیست."));
            }

            // 🟢 ذخیره واقعی آگهی در دیتابیس SQLite
            System.out.println("📝 آگهی جدید از کاربر " + username + " دریافت شد: " + advertisementData);
            Advertisement savedAd = advertisementService.saveAdvertisement(advertisementData, username);

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

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی شما با موفقیت ثبت شد.",
                    "token", newToken,
                    "data", savedAd
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

    /**
     * 📋 ۳. دریافت آگهی‌های اختصاصی خود کاربر (GET /my)
     */
    @GetMapping("/advertisements/my")
    public ResponseEntity<?> getMyAdvertisements(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "توکن یافت نشد."));
        }
        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token, username) || !authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما معتبر نیست."));
            }

            // 🟢 دریافت آگهی‌های فیلتر شده بر اساس مالک آگهی
            List<Advertisement> myAds = advertisementService.getAdsByUsername(username);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی‌های شما با موفقیت بارگذاری شد.",
                    "data", myAds
            ));
        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) authService.logout(expiredUsername);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما منقضی شده است."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "خطا در دریافت آگهی‌های شما."));
        }
    }

    /**
     * ✏️ ۴. ویرایش آگهی (PUT) + تمدید توکن برای تجربه کاربری بهتر
     */
    @PutMapping("/advertisements/{id}")
    public ResponseEntity<?> updateAdvertisement(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> updatedData,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "دسترسی غیرمجاز."));
        }
        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token, username) || !authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست معتبر نیست."));
            }

            // 🔍 پیدا کردن آگهی موجود
            Optional<Advertisement> adOpt = advertisementService.getAdById(id);
            if (adOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "آگهی مورد نظر یافت نشد."));
            }

            Advertisement existingAd = adOpt.get();

            // 🛡️ بررسی امنیتی فوق‌العاده مهم: تطابق مالک آگهی با توکن ارسال شده
            if (!existingAd.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "شما اجازه ویرایش این آگهی را ندارید!"));
            }

            // 🟢 ویرایش و ذخیره آگهی
            Advertisement updatedAd = advertisementService.updateAdvertisement(existingAd, updatedData);

            // 🔄 تمدید توکن در عملیات ویرایش
            String newToken = jwtUtil.generateToken(username);
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setToken(newToken);
                userRepository.saveAndFlush(user);
            });
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی با موفقیت ویرایش شد.",
                    "token", newToken,
                    "data", updatedAd
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) authService.logout(expiredUsername);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما منقضی شده است."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "خطا در ویرایش آگهی."));
        }
    }

    /**
     * 🗑️ ۵. حذف آگهی (DELETE)
     */
    @DeleteMapping("/advertisements/{id}")
    public ResponseEntity<?> deleteAdvertisement(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "دسترسی غیرمجاز."));
        }
        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null || !jwtUtil.validateToken(token, username) || !authService.isTokenValidInDb(username, token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست معتبر نیست."));
            }

            // 🔍 پیدا کردن آگهی
            Optional<Advertisement> adOpt = advertisementService.getAdById(id);
            if (adOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "آگهی یافت نشد."));
            }

            Advertisement ad = adOpt.get();

            // 🛡️ بررسی امنیتی فوق‌العاده مهم: فقط مالک آگهی می‌تواند آن را حذف کند
            if (!ad.getOwnerUsername().equals(username)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "شما اجازه حذف این آگهی را ندارید!"));
            }

            // 🟢 حذف فیزیکی از دیتابیس
            advertisementService.deleteAd(id);
            System.out.println("🗑️ آگهی شماره " + id + " با موفقیت توسط " + username + " حذف شد.");

            // 🔄 تمدید توکن در عملیات حذف
            String newToken = jwtUtil.generateToken(username);
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setToken(newToken);
                userRepository.saveAndFlush(user);
            });
            response.setHeader("Authorization", "Bearer " + newToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization");

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "آگهی با موفقیت حذف شد.",
                    "token", newToken
            ));

        } catch (ExpiredJwtException e) {
            String expiredUsername = e.getClaims().getSubject();
            if (expiredUsername != null) authService.logout(expiredUsername);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "نشست شما منقضی شده است."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "خطا در حذف آگهی."));
        }
    }
}