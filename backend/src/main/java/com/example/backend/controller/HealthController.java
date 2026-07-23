package com.example.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ❤️ اندپوینت سلامت سرور (Health Check) مطابق سند راهنمای پروژه:
 * GET /api/health
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HealthController {

    /**
     * متد «health»؛ بخشی از عملکرد کلاس HealthController را پیاده‌سازی می‌کند.
     *
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "سرور در حال اجرا است."
        ));
    }
}
