package com.example.backend.controller;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.security.JwtUtil;
import com.example.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ورود، ثبت‌نام و خروج کاربران
 */
@RestController
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * ورود کاربر به سامانه.
     *
     * @param request شیء درخواست HTTP
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage(), "status", 401));
        }
    }

    /**
     * ثبت مقدار مربوطه جدید.
     *
     * @param request شیء درخواست HTTP
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            authService.registerNewUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "ثبت‌نام با موفقیت انجام شد.", "status", 201));
        } catch (IllegalArgumentException e) {
            // فرانت‌اند متن body را مستقیم نمایش می‌دهد
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * خروج کاربر از حساب کاربری.
     *
     * @param authHeader هدر Authorization
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String username = jwtUtil.extractUsername(authHeader.substring(7));
                if (username != null) {
                    authService.logout(username);
                }
            } catch (Exception ignored) {
                // توکن نامعتبر یا منقضی — خروج بدون خطا
            }
        }
        return ResponseEntity.ok(Map.of("message", "با موفقیت از حساب خارج شدید.", "status", 200));
    }
}
