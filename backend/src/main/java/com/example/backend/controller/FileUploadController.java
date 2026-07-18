package com.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")   // ✅ اصلاح شد: تغییر آدرس پایه به /upload تا با فرانت‌انند یکی شود
@CrossOrigin(origins = "*")
public class FileUploadController {

    // پوشه‌ای که عکس‌ها در آن ذخیره می‌شوند
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    @PostMapping // ✅ اصلاح شد: حذف /upload اضافی تا آدرس نهایی دقیقاً localhost:8080/upload شود
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "فایل ارسال شده خالی است."));
        }

        try {
            // ۱. مطمئن شدن از وجود پوشه uploads
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // ۲. تمیزکاری نام فایل و تولید نام غیرتکراری با UUID
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // ۳. ذخیره فیزیکی بایت‌های فایل روی هارد سرور
            Path path = Paths.get(UPLOAD_DIR + newFileName);
            Files.write(path, file.getBytes());

            // ۴. برگشت دادن آدرس عکس با کلید image_url (دقیقاً همان چیزی که فرانت‌انند در extractImageUrlFromJson دنبالش می‌گردد)
            String imageUrl = "/uploads/" + newFileName;
            System.out.println("💾 [BACKEND] فایل با موفقیت ذخیره شد: " + imageUrl);

            return ResponseEntity.ok(Map.of("image_url", imageUrl, "imageUrl", imageUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "SERVER_ERROR", "message", "خطا در ذخیره فایل: " + e.getMessage()));
        }
    }
}