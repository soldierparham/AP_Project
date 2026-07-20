package com.example.backend.service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 🔐 جدید: هش کردن رمز عبور با BCrypt (مطابق بخش امنیت سند پروژه)
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isPhoneNumberExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * ثبت‌نام کاربر جدید همراه با اعتبارسنجی + هش شدن رمز عبور
     */
    @Transactional
    public void registerNewUser(RegisterRequest dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("این نام کاربری قبلاً توسط کاربر دیگری انتخاب شده است.");
        }

        if (userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("این شماره تماس قبلاً در سیستم ثبت شده است.");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEmail(dto.getEmail());
        // 🔐 ذخیره رمز به صورت هش‌شده (دیگر رمز خام در دیتابیس ذخیره نمی‌شود)
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole("USER");
        user.setStatus("ACTIVE");

        userRepository.saveAndFlush(user);
    }

    /**
     * 🔐 مدیریت منطق ورود (لاگین) و صدور توکن JWT
     * - کاربران مسدودشده (BLOCKED) اجازه ورود ندارند
     * - پشتیبانی از رمزهای قدیمی متنی (مهاجرت خودکار به BCrypt در اولین ورود موفق)
     */
    @Transactional
    public AuthResponse login(LoginRequest dto) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(dto.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if ("BLOCKED".equalsIgnoreCase(user.getStatus())) {
                throw new IllegalArgumentException("حساب کاربری شما توسط مدیر سیستم مسدود شده است.");
            }

            if (matchesPassword(dto.getPassword(), user.getPassword())) {
                // 🔄 مهاجرت رمزهای متنی قدیمی به هش BCrypt
                if (!isBcryptHash(user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(dto.getPassword()));
                }

                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

                user.setJwtToken(token);
                userRepository.saveAndFlush(user);

                return new AuthResponse(true, "ورود موفقیت‌آمیز بود.", token);
            }
        }

        throw new IllegalArgumentException("شماره تماس یا رمز عبور اشتباه است.");
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

    /**
     * 🛡️ بررسی صحت وجود توکن در دیتابیس (بدون ایجاد تغییر در آن)
     */
    @Transactional(readOnly = true)
    public boolean isTokenValidInDb(String username, String incomingToken) {
        if (username == null || incomingToken == null) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByUsername(username)
                .or(() -> userRepository.findByPhoneNumber(username));

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getJwtToken() != null && user.getJwtToken().equals(incomingToken);
        }
        return false;
    }

    /**
     * 🔍 اعتبارسنجی سشن و بررسی هوشمند انقضای توکن
     */
    @Transactional
    public boolean checkSession(String identifier, String incomingToken) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByUsername(identifier));

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getJwtToken() == null || !user.getJwtToken().equals(incomingToken)) {
                return false;
            }

            try {
                boolean isValid = jwtUtil.validateToken(incomingToken, user.getUsername());
                return isValid;
            } catch (ExpiredJwtException e) {
                user.setJwtToken(null);
                userRepository.saveAndFlush(user);
                System.out.println("🧹 [CheckSession] توکن منقضی شده کاربر " + user.getUsername() + " در دیتابیس null شد.");
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 🔒 خروج از حساب (پاک کردن توکن دیتابیس)
     */
    @Transactional
    public void logout(String identifier) {
        Optional<User> userOpt = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByUsername(identifier));

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setJwtToken(null);
            userRepository.saveAndFlush(user);
            System.out.println("📌 دیتابیس به‌روزرسانی شد: توکن کاربر " + user.getUsername() + " با موفقیت null شد.");
        }
    }
}
