package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 📷 آپلود تصویر آگهی — نسخه تکمیل‌شده:
 * - پشتیبانی همزمان از کلید "file" (تک‌فایل) و "files" (چند فایل — گالری تصاویر)
 *   ⚠️ رفع باگ قبلی: فرانت (uploadMultipleFiles) فایل‌ها را با کلید "files" می‌فرستاد
 *   ولی بک‌اند فقط کلید "file" را می‌پذیرفت و آپلود چندتصویری شکست می‌خورد.
 * - خروجی: مسیرهای فایل‌ها با کاما از هم جدا می‌شوند (مثل /uploads/a.jpg,/uploads/b.jpg)
 */
@RestController
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    // ⚠️ فرانت‌اند به /upload (بدون /api) پست می‌کند؛ هر دو مسیر پذیرفته می‌شوند
    @PostMapping({"/upload", "/api/upload"})
    public ResponseEntity<?> uploadFiles(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        List<MultipartFile> allFiles = new ArrayList<>();
        if (files != null) {
            allFiles.addAll(files);
        }
        if (file != null) {
            allFiles.add(file);
        }
        allFiles.removeIf(f -> f == null || f.isEmpty());

        if (allFiles.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "هیچ فایلی برای آپلود ارسال نشده است.", "status", 400));
        }

        try {
            Files.createDirectories(Path.of(UPLOAD_DIR));

            List<String> savedUrls = new ArrayList<>();
            for (MultipartFile f : allFiles) {
                String originalName = f.getOriginalFilename();
                String extension = "";
                if (originalName != null && originalName.contains(".")) {
                    extension = originalName.substring(originalName.lastIndexOf('.'));
                }
                String newFileName = UUID.randomUUID() + extension;

                File destination = new File(UPLOAD_DIR + newFileName);
                f.transferTo(destination);

                savedUrls.add("/uploads/" + newFileName);
            }

            String joinedUrls = String.join(",", savedUrls);

            // هر دو کلید image_url و imageUrl برای سازگاری با فرانت برگردانده می‌شوند
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "فایل(ها) با موفقیت آپلود شد.",
                    "image_url", joinedUrls,
                    "imageUrl", joinedUrls
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "خطا در ذخیره فایل: " + e.getMessage(), "status", 500));
        }
    }
}
