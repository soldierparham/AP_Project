package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.RatingRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * مدیریت مشخصات کاربر جاری (نمایش و ویرایش پروفایل از بخش «بازارچه من»)
 * - نام کاربری قابل تغییر است و همه ارجاعات و توکن خودکار بروزرسانی می‌شوند (آگهی‌ها، چت‌ها و توکن به آن گره خورده‌اند)
 * - تغییر رمز فقط با تأیید رمز فعلی انجام می‌شود
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

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
        // \ud83d\udcf5 شماره تماس شناسه یکتای حساب است و قابل تغییر نیست
        if (!phone.isBlank() && !phone.equals(user.getPhoneNumber())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شماره تماس شناسه حساب شماست و قابل تغییر نیست.", "status", 400));
        }

        // تغییر نام کاربری (شماره تماس شناسه اصلی حساب است، پس نام کاربری آزاد است)
        String newUsername = body.get("username") == null ? "" : body.get("username").trim();
        String rotatedToken = null;
        if (!newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            if (!newUsername.matches("[A-Za-z0-9_.\\-]{3,30}")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "نام کاربری باید بین ۳ تا ۳۰ کاراکتر و فقط شامل حروف انگلیسی، عدد، نقطه، خط تیره و زیرخط باشد.", "status", 400));
            }
            if (userRepository.existsByUsername(newUsername)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "این نام کاربری قبلاً توسط کاربر دیگری انتخاب شده است.", "status", 400));
            }

            String oldUsername = user.getUsername();

            // بروزرسانی همه ارجاعات نام کاربری در سراسر سیستم
            advertisementRepository.findByOwnerUsername(oldUsername).forEach(ad -> {
                ad.setOwnerUsername(newUsername);
                advertisementRepository.save(ad);
            });
            ratingRepository.findAll().forEach(r -> {
                boolean changed = false;
                if (oldUsername.equals(r.getRaterUsername())) { r.setRaterUsername(newUsername); changed = true; }
                if (oldUsername.equals(r.getSellerUsername())) { r.setSellerUsername(newUsername); changed = true; }
                if (changed) {
                    ratingRepository.save(r);
                }
            });
            favoriteRepository.findByUsernameOrderByCreatedAtDesc(oldUsername).forEach(f -> {
                f.setUsername(newUsername);
                favoriteRepository.save(f);
            });
            conversationRepository.findByBuyerUsernameOrSellerUsername(oldUsername, oldUsername).forEach(c -> {
                if (oldUsername.equals(c.getBuyerUsername())) { c.setBuyerUsername(newUsername); }
                if (oldUsername.equals(c.getSellerUsername())) { c.setSellerUsername(newUsername); }
                conversationRepository.save(c);
            });
            messageRepository.findAll().forEach(msg -> {
                if (oldUsername.equals(msg.getSenderUsername())) {
                    msg.setSenderUsername(newUsername);
                    messageRepository.save(msg);
                }
            });

            user.setUsername(newUsername);

            // توکن قبلی به نام کاربری قدیمی گره خورده؛ توکن جدید صادر می‌شود
            rotatedToken = jwtUtil.generateToken(newUsername, user.getRole());
            user.setJwtToken(rotatedToken);
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
        // شماره تماس عمداً بروز نمی‌شود (شناسه حساب)
        user.setEmail(email);
        userRepository.saveAndFlush(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("message", "مشخصات شما با موفقیت به‌روزرسانی شد.");
        if (rotatedToken != null) {
            // فرانت‌اند توکن و نام کاربری جدید را جایگزین می‌کند
            result.put("token", rotatedToken);
            result.put("username", user.getUsername());
        }
        return ResponseEntity.ok(result);
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
