package com.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HelloController {

    public static final String BASE_URL = "http://localhost:8080";

    @FXML
    private Button myDivarButton; // تزریق دکمه از فایل FXML

    private final ContextMenu hoverMenu = new ContextMenu();

    /**
     * متد مقداردهی اولیه جاوا اف‌ایکس برای ساخت منوی هاور
     */
    @FXML
    public void initialize() {
        // ۱. ساخت گزینه‌های منو
        MenuItem profileItem = new MenuItem("👤 پروفایل کاربری");
        MenuItem logoutItem = new MenuItem("🔒 خروج از حساب");

        // ۲. استایل‌دهی شیک هماهنگ با تم تاریک برنامه
        hoverMenu.setStyle(
                "-fx-background-color: #241942;" +
                        "-fx-border-color: #3b286b;" +
                        "-fx-border-width: 1px;" +
                        "-fx-border-radius: 6px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-padding: 5px;"
        );

        profileItem.setStyle("-fx-text-fill: white; -fx-font-family: 'Segoe UI', 'Vazirmatn'; -fx-font-size: 13px; -fx-cursor: hand;");
        logoutItem.setStyle("-fx-text-fill: #ff5555; -fx-font-family: 'Segoe UI', 'Vazirmatn'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");

        hoverMenu.getItems().addAll(profileItem, logoutItem);

        // ۳. تنظیم اکشن کلیک گزینه‌ها
        profileItem.setOnAction(event -> handleProfileClick());
        logoutItem.setOnAction(event -> handleLogoutClick());

        // ۴. نمایش منو به محض رفتن موس روی دکمه "دیوار من"
        myDivarButton.setOnMouseEntered(event -> {
            if (!hoverMenu.isShowing()) {
                hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 2);
            }
        });
    }

    /**
     * 👤 عملیات کلیک روی پروفایل
     */
    private void handleProfileClick() {
        System.out.println("نمایش مشخصات کاربر: " + MainApplication.currentUsername);
    }

    /**
     * 🔒 عملیات خروج، ابطال توکن بک‌انند و انتقال به صفحه لاگین
     */
    private void handleLogoutClick() {
        String username = MainApplication.currentUsername;

        if (username != null) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/logout?username=" + username))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(e -> {
                        System.err.println("خطا در ارتباط با سرور برای خروج: " + e.getMessage());
                        return null;
                    });
        }

        Stage currentStage = (Stage) myDivarButton.getScene().getWindow();
        MainApplication.redirectToLogin(currentStage, "شما با موفقیت از حساب کاربری خود خارج شدید.");
    }

    /**
     * 📝 متد ارسال درخواست ثبت آگهی و تمدید خودکار توکن در فرانت‌انند
     */
    @FXML
    private void onRegisterAdClick() {
        HttpClient client = HttpClient.newHttpClient();

        // 📦 ساخت دیتای ساختگی آگهی در قالب JSON برای ارسال به بک‌انند
        String jsonBody = "{"
                + "\"title\": \"آگهی فروش گوشی\","
                + "\"description\": \"یک دستگاه گوشی در حد نو\","
                + "\"price\": 15000000"
                + "}";

        // 🛠️ اصلاح شد: تبدیل به متد POST و ارسال بدنه JSON به همراه هدرهای لازم
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)) // ارسال با متد POST
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    String body = response.body();

                    // 🔄 تمدید توکن (Sliding Expiration): استخراج توکن جدید از هدر پاسخ بک‌انند
                    response.headers().firstValue("Authorization").ifPresent(authHeader -> {
                        if (authHeader.startsWith("Bearer ")) {
                            String newToken = authHeader.substring(7);
                            MainApplication.jwtToken = newToken; // 🌟 ذخیره توکن تازه‌نفس برای درخواست‌های بعدی
                            System.out.println("🔄 [JavaFX] توکن جدید با موفقیت دریافت و در برنامه تمدید شد!");
                        }
                    });

                    Platform.runLater(() -> {
                        // 🛡️ بررسی وضعیت منقضی شدن توکن (خطای 401)
                        if (statusCode == 401) {
                            Stage currentStage = (Stage) myDivarButton.getScene().getWindow();
                            MainApplication.redirectToLogin(currentStage, "نشست شما به پایان رسیده است. لطفاً مجدداً وارد شوید.");
                            return;
                        }

                        // در صورت ثبت موفقیت آمیز آگهی (کد 200)
                        if (statusCode == 200) {
                            System.out.println("پاسخ از بک‌اند: " + body);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("وضعیت ثبت آگهی");
                            alert.setHeaderText(null);
                            alert.setContentText("آگهی با موفقیت ثبت شد و زمان نشست شما تمدید گردید!");
                            alert.show();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("خطای سرور");
                            alert.setHeaderText(null);
                            alert.setContentText("سرور خطای غیرمنتظره‌ای با کد " + statusCode + " برگرداند.");
                            alert.show();
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("خطا در اتصال");
                        alert.setHeaderText(null);
                        alert.setContentText("خطا در ارتباط با سرور: " + e.getMessage());
                        alert.show();
                    });
                    return null;
                });
    }
}