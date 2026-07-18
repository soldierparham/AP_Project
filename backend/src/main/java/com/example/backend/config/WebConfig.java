package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // مسیر فیزیکی پوشه آپلودها (دقت کنید که یک اسلش در انتها دارد)
        String uploadPath = System.getProperty("user.dir") + "/uploads/";

        // اتصال درخواست‌های وب به پوشه فیزیکی روی هارد دیسک
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath); // ✅ اسلش اضافی انتها حذف شد

        System.out.println("🌐 [BACKEND] مسیر استاتیک عکس‌ها فعال شد: file:" + uploadPath);
    }
}