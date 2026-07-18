package com.example.frontend.service;

import com.example.frontend.MainApplication;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
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
     * 📥 ارسال درخواست GET به صورت ناهمگام (Async)
     * مناسب برای دریافت لیست آگهی‌ها
     */
    public static CompletableFuture<HttpResponse<String>> sendGet(String endpoint) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + endpoint))
                .GET();

        // 🔑 بررسی هوشمند: اگر توکن در برنامه ذخیره شده بود، آن را به هدر اضافه کن
        addAuthorizationHeader(requestBuilder);

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 📸 آپلود یک فایل به صورت ناهمگام (Multipart POST Async)
     */
    public static CompletableFuture<HttpResponse<String>> uploadFile(String endpoint, File file) {
        CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
        try {
            String boundary = "JavaFX-Boundary-" + UUID.randomUUID().toString();
            byte[] multipartBody = createMultipartBody(file, boundary);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody));

            addAuthorizationHeader(requestBuilder);

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(future::complete)
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 📸🚀 آپلود چندین فایل به صورت هم‌زمان و ناهمگام (Multipart POST Async)
     * فایل‌ها با کلید یکسان "files" که بک‌انند منتظر آن است فرستاده می‌شوند.
     */
    public static CompletableFuture<HttpResponse<String>> uploadMultipleFiles(String endpoint, List<File> files) {
        CompletableFuture<HttpResponse<String>> future = new CompletableFuture<>();
        try {
            String boundary = "JavaFX-Boundary-" + UUID.randomUUID().toString();
            byte[] multipartBody = createMultipleMultipartBody(files, boundary);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody));

            // هدر امنیت توکن برای آپلود چندگانه نیز اعمال می‌شود
            addAuthorizationHeader(requestBuilder);

            httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenAccept(future::complete)
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * ⚙️ متد کمکی برای ساخت بدنه پکت استاندارد Multipart/Form-Data به صورت بایتی (تک فایلی)
     */
    private static byte[] createMultipartBody(File file, String boundary) throws IOException {
        String fileName = file.getName();
        String mimeType = getMimeType(file);

        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

        return body;
    }

    /**
     * ⚙️🛠️ متد کمکی جدید برای سرهم‌کردن پکت بایتیِ چندین فایل در یک درخواست
     */
    private static byte[] createMultipleMultipartBody(List<File> files, String boundary) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] boundaryBytes = ("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] lineEndBytes = "\r\n".getBytes(StandardCharsets.UTF_8);

        for (File file : files) {
            String fileName = file.getName();
            String mimeType = getMimeType(file);

            // ۱. نوشتن خط شروع باندری برای این فایل
            outputStream.write(boundaryBytes);

            // ۲. نوشتن هدر مربوط به فایل (دقت کنید کلید حتماً "files" باشد)
            String fileHeader = "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"\r\n" +
                    "Content-Type: " + mimeType + "\r\n\r\n";
            outputStream.write(fileHeader.getBytes(StandardCharsets.UTF_8));

            // ۳. نوشتن محتوای بایتی خود فایل
            outputStream.write(Files.readAllBytes(file.toPath()));

            // ۴. اضافه کردن خط پایان برای فایل جاری
            outputStream.write(lineEndBytes);
        }

        // ۵. انتهای کل پکت مالتی‌پارت
        String footer = "--" + boundary + "--\r\n";
        outputStream.write(footer.getBytes(StandardCharsets.UTF_8));

        return outputStream.toByteArray();
    }

    /**
     * 🔍 متد کمکی برای تشخیص پسوند و MimeType فایل
     */
    private static String getMimeType(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType != null) return mimeType;
        } catch (Exception e) {
            // در صورت بروز خطا به مقادیر پیش‌فرض سوئیچ می‌کند
        }
        return "image/jpeg";
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