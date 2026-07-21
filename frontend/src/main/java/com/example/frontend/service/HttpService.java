package com.example.frontend.service;

import com.example.frontend.MainApplication;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.concurrent.CompletableFuture;

public class HttpService {

    // 🌐 آدرس پایه بک‌انند شما
    private static final String BASE_URL = "http://localhost:8080/api";

    // ایجاد یک نمونه HttpClient به صورت یکتا (Singleton) برای بهینه‌سازی مصرف منابع
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 📤 ارسال درخواست POST به صورت ناهمگام (Async)
     * مناسب برای لاگین، ثبت‌نام و ثبت آگهی جدید
     */
    public static CompletableFuture<HttpResponse<String>> sendPost(String endpoint, String jsonPayload) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        // 🔑 بررسی هوشمند: اگر توکن در برنامه ذخیره شده بود، آن را به هدر اضافه کن
        addAuthorizationHeader(requestBuilder);

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }


    /**
     * 🛡️ متد کمکی برای تزریق توکن به هدر درخواست‌ها (در صورت وجود)
     */
    private static void addAuthorizationHeader(HttpRequest.Builder requestBuilder) {
        if (MainApplication.jwtToken != null && !MainApplication.jwtToken.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + MainApplication.jwtToken);
        }
    }
}