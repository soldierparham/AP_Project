package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 🚀 نقطه شروع برنامه بک‌اند (این کلاس در نسخه قبلی پروژه وجود نداشت و بدون آن برنامه اجرا نمی‌شود!)
 * اجرای برنامه: mvn spring-boot:run (پورت 8080)
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
