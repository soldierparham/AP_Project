package com.example.frontend;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.http.HttpClient;

public class MainApplication extends Application {

    // آدرس پایه بک‌اند (همان پورتی که بک‌اند اجرا شده)
    public static final String BASE_URL = "http://localhost:8080";

    @Override
    public void start(Stage stage) {
        Label label = new Label("فرانت‌اند سامانه دست دوم");
        Button testBtn = new Button("اتصال به بک‌اند");

        // با کلیک دکمه، یک درخواست تست به بک‌اند می‌زنیم
        testBtn.setOnAction(e -> testConnection());

        VBox root = new VBox(15, label, testBtn);
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 400, 300);
        stage.setTitle("Second-hand App");
        stage.setScene(scene);
        stage.show();
    }

    private void testConnection() {
        // یک HttpClient ساده برای درخواست GET به لیست آگهی‌ها (یا هر endpoint عمومی)
        HttpClient client = HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(BASE_URL + "/api/advertisements"))
                .GET()
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenApply(java.net.http.HttpResponse::body)
                .thenAccept(body -> {
                    // نمایش نتیجه در یک پنجره popup ساده (یا System.out)
                    System.out.println("پاسخ از بک‌اند: " + body);
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setContentText("اتصال موفق! پاسخ: " + body.substring(0, Math.min(50, body.length())) + "...");
                    alert.show();
                })
                .exceptionally(e -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setContentText("خطا در اتصال به بک‌اند: " + e.getMessage());
                    alert.show();
                    return null;
                });
    }

    public static void main(String[] args) {
        launch(args);
    }
}