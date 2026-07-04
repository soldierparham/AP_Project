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
    public static String jwtToken = null; // 👈 ۱. اضافه شد تا فرانت‌انند بتواند توکن را مدیریت کند

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/com/example/frontend/login-view.fxml"));
            Parent root = fxmlLoader.load();

            // 👈 ۲. اصلاح شد: تعیین دقیق ابعاد اولیه روی 600 در 800 برای رفع مشکل تغییر سایز پس از لاگ‌اوت
            Scene scene = new Scene(root, 600, 800);

            stage.setTitle("سامانه دست دوم (طرح دیوار)");
            stage.setScene(scene);
            stage.setResizable(false); // غیرفعال کردن ریسایز دستی پنجره ورود

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
     * 🔄 ۳. متد متمرکز برای انتقال کاربر به صفحه لاگین (با ابعاد دقیق و پاکسازی سشن)
     * این متد هم در دکمه خروج و هم هنگام اکسپایر شدن توکن (خطای 401) صدا زده می‌شود.
     */
    public static void redirectToLogin(Stage currentStage, String warningMessage) {
        Platform.runLater(() -> {
            try {
                // پاکسازی کامل متغیرهای سشن محلی
                currentUsername = null;
                jwtToken = null;

                // بستن پنجره اصلی (داشبورد)
                if (currentStage != null) {
                    currentStage.close();
                }

                // بارگذاری مجدد پنجره لاگین با سایز دقیق و هماهنگ اولیه
                FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("/com/example/frontend/login-view.fxml"));
                Parent root = fxmlLoader.load();
                Scene scene = new Scene(root, 600, 800); // 🌟 تضمین ثبات ابعاد

                Stage loginStage = new Stage();
                loginStage.setTitle("ورود به سامانه دیوار");
                loginStage.setScene(scene);
                loginStage.setResizable(false);
                loginStage.show();

                // نمایش هشدار مناسب به کاربر
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("وضعیت حساب");
                alert.setHeaderText(null);
                alert.setContentText(warningMessage);
                alert.show();

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("خطا در انتقال فرآیند به صفحه لاگین!");
            }
        });
    }

    /**
     * 🌐 ارسال درخواست خروج فوری به بک‌انند هنگام بستن پنجره با ضربدر
     */
    private void sendLogoutRequestToBackend(String username) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // ارسال درخواست به اندپوینتی که در مرحله قبل اصلاح کردیم
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/logout?username=" + username))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            // استفاده از ارسال همزمان (Sync) به دلیل بسته شدن آنی پروسس برنامه
            client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("🔒 توکن کاربر در دیتابیس بک‌انند null شد.");

        } catch (Exception e) {
            System.err.println("❌ خطا در باطل کردن توکن بک‌انند: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}