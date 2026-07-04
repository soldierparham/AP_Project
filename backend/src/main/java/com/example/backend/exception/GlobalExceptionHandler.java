package com.example.backend.exception;

import com.example.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // فعال‌سازی مدیریت هوشمند و جهانی خطاها بر روی تمام کنترلرها
public class GlobalExceptionHandler {

    /**
     * مدیریت خطای داده‌های تکراری یا ورودی‌های نامعتبر (استاتوس ۴۰۹ و ۴۰۰)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        // تشخیص نوع خطا بر اساس متن آن برای فرستادن کد مناسب به فرانت
        String errorCode = "VALIDATION_ERROR";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (ex.getMessage().contains("تکراری") || ex.getMessage().contains("قبلاً")) {
            errorCode = "DUPLICATE_ERROR";
            status = HttpStatus.CONFLICT; // کد 409
        }

        ErrorResponse error = new ErrorResponse(errorCode, ex.getMessage());
        return new ResponseEntity<>(error, status);
    }

    /**
     * مدیریت خطاهای پیش‌بینی نشده سرور (استاتوس ۵۰۰)
     * برای اینکه برنامه تحت هیچ شرایطی کرش نکند و متوقف نشود
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                "SERVER_ERROR",
                "مشکلی در سرور رخ داده است. لطفاً بعداً تلاش کنید."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}