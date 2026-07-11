package com.example.frontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MainApplication extends Application {

    // 🌟 ذخیره اطلاعات سشن جاری کاربر در کل فرانت‌انند
    public static String currentUsername = null;
    public static String jwtToken = null;

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/com/example/frontend/login-view.fxml"));
            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 600, 800); // 🌟 تضمین ثبات ابعاد اولیه

            stage.setTitle("سامانه دست دوم (طرح دیوار)");
            stage.setScene(scene);
            stage.setResizable(false);

            // 🚪 مدیریت بسته شدن ناگهانی برنامه توسط کاربر
            stage.setOnCloseRequest(event -> {
                System.out.println("🔴 پنجره برنامه توسط کاربر بسته شد.");
                if (currentUsername != null) {
                    sendLogoutRequestToBackend(currentUsername);
                }
                Platform.exit();
                System.exit(0);
            });

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("خطا در بارگذاری فایل FXML! مسیر فایل را چک کنید.");
        }
    }

    /**
     * 🔄 متد متمرکز برای انتقال کاربر به صفحه لاگین (با ابعاد دقیق و پاکسازی سشن)
     */
    public static void redirectToLogin(Stage currentStage, String warningMessage) {
        Platform.runLater(() -> {
            try {
                // ۱. پاکسازی کامل متغیرهای سشن محلی
                currentUsername = null;
                jwtToken = null;

                // ۲. بستن پنجره اصلی (داشبورد)
                if (currentStage != null) {
                    currentStage.close();
                }

                // ۳. بارگذاری مجدد پنجره لاگین
                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/com/example/frontend/login-view.fxml"));
                Parent root = fxmlLoader.load();
                Scene scene = new Scene(root, 600, 800);

                Stage loginStage = new Stage();
                loginStage.setTitle("ورود به سامانه دیوار");
                loginStage.setScene(scene);
                loginStage.setResizable(false);
                loginStage.show();

                // 📌 اصلاح: تنظیم مالک آلرت برای جلوگیری از رفتن به زیر پنجره و استفاده از showAndWait
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.initOwner(loginStage); // 👈 اتصال آلرت به پنجره لاگین
                alert.setTitle("وضعیت حساب");
                alert.setHeaderText(null);
                alert.setContentText(warningMessage);
                alert.showAndWait(); // 👈 انتظار برای کلیک کاربر

            } catch (Exception e) {
                System.err.println("❌ خطا در انتقال فرآیند به صفحه لاگین: " + e.getMessage());
            }
        });
    }

    /**
     * 🌐 ارسال درخواست خروج فوری به بک‌انند هنگام بستن پنجره با ضربدر
     */
    private void sendLogoutRequestToBackend(String username) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/logout?username=" + username))
                    .POST(HttpRequest.BodyPublishers.noBody());

            // 🔐 نکته امنیتی پیوند: اگر اندپوینت خروج بک‌انند شما فیلتر JWT دارد، خط زیر را فعال کنید:
            // if (jwtToken != null) { requestBuilder.header("Authorization", "Bearer " + jwtToken); }

            HttpRequest request = requestBuilder.build();

            // استفاده از ارسال همزمان (Sync) به دلیل بسته شدن آنی پروسس برنامه کاملاً درست است
            client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("🔒 توکن کاربر در دیتابیس بک‌انند با موفقیت باطل شد.");

        } catch (Exception e) {
            System.err.println("❌ خطا در باطل کردن توکن بک‌انند: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}