package com.example.backend.controller;

import com.example.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/**
 * 🗂️ مشاهده لیست دسته‌بندی‌ها (برای همه کاربران واردشده)
 * مدیریت (افزودن/حذف) دسته‌بندی‌ها در AdminController انجام می‌شود.
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<?> getAllCategories(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", categoryRepository.findAll()
        ));
    }
}
