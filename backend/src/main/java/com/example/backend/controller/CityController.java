package com.example.backend.controller;

import com.example.backend.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * 🏙️ مشاهده لیست شهرها (برای همه کاربران واردشده)
 * مدیریت (افزودن/ویرایش/حذف) شهرها در AdminController انجام می‌شود.
 */
@RestController
@RequestMapping("/api/cities")
public class CityController {

    @Autowired
    private CityRepository cityRepository;

    /**
     * مقدار «all cities» را برمی‌گرداند.
     *
     * @param principal پارامتر principal
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @GetMapping
    public ResponseEntity getAllCities(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", cityRepository.findAll()
        ));
    }
}
