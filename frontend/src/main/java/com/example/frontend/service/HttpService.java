package com.example.frontend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/** سرویس مشترک ارسال درخواست‌های REST به بک‌اند */
public class HttpService {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * «post» را ارسال می‌کند.
     *
     * @param path پارامتر path
     * @param jsonBody پارامتر jsonBody
     * @return مقدار بازگشتی
     */
    public static CompletableFuture<HttpResponse<String>> sendPost(String path, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
