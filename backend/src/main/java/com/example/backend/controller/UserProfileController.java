package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * مدیریت مشخصات کاربر جاری (نمایش و ویرایش پروفایل از بخش «بازارچه من»)
 * - نام کاربری غیرقابل تغییر است (آگهی‌ها، چت‌ها و توکن به آن گره خورده‌اند)
 * - تغییر رمز فقط با تأیید رمز فعلی انجام می‌شود
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Optional<User> findCurrentUser(Principal principal) {
        if (principal == null) return Optional.empty();
        return userRepository.findByUsername(principal.getName())
                .or(() -> userRepository.findByPhoneNumber(principal.getName()));
    }

    /** دریافت مشخصات کاربر جاری */
    @GetMapping("/me")
    public ResponseEntity getMyProfile(Principal principal) {
        Optional<User> userOpt = findCurrentUser(principal);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری شوید.", "status", 401));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "name", user.getName() == null ? "" : user.getName(),
                        "username", user.getUsername() == null ? "" : user.getUsername(),
                        "phoneNumber", user.getPhoneNumber() == null ? "" : user.getPhoneNumber(),
                        "email", user.getEmail() == null ? "" : user.getEmail()
                )
        ));
    }

    /** ویرایش مشخصات کاربر جاری */
    @Transactional
    @PutMapping("/me")
    public ResponseEntity updateMyProfile(@RequestBody Map<String, String> body, Principal principal) {
        Optional<User> userOpt = findCurrentUser(principal);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری شوید.", "status", 401));
        }
        User user = userOpt.get();

        String name = body.get("name") == null ? "" : body.get("name").trim();
        String phone = body.get("phoneNumber") == null ? "" : body.get("phoneNumber").trim();
        String email = body.get("email") == null ? "" : body.get("email").trim();
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "نام نمی‌تواند خالی باشد.", "status", 400));
        }
        if (phone.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شماره تماس نمی‌تواند خالی باشد.", "status", 400));
        }

        // شماره تماس جدید نباید متعلق به کاربر دیگری باشد
        if (!phone.equals(user.getPhoneNumber()) && userRepository.existsByPhoneNumber(phone)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "این شماره تماس قبلاً توسط کاربر دیگری ثبت شده است.", "status", 400));
        }

        // تغییر رمز عبور (اختیاری)
        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || currentPassword.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "برای تغییر رمز عبور، رمز عبور فعلی را وارد کنید.", "status", 400));
            }
            if (!matchesPassword(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "رمز عبور فعلی اشتباه است.", "status", 400));
            }
            if (newPassword.length() < 4) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "رمز عبور جدید باید حداقل ۴ کاراکتر باشد.", "status", 400));
            }
            if (newPassword.contains(" ")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "رمز عبور نباید شامل فاصله باشد.", "status", 400));
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setName(name);
        user.setPhoneNumber(phone);
        user.setEmail(email);
        userRepository.saveAndFlush(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "مشخصات شما با موفقیت به‌روزرسانی شد."
        ));
    }

    private boolean isBcryptHash(String stored) {
        return stored != null && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        // سازگاری با حساب‌های قدیمی که رمزشان متنی ذخیره شده بود
        return storedPassword.equals(rawPassword);
    }
}
