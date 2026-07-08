package com.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node; // ➕ اضافه شدن برای ذخیره حالت صفحه
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HelloController {

    public static final String BASE_URL = "http://localhost:8080";

    @FXML
    private BorderPane mainBorderPane; // 🌟 تزریق کانتینر اصلی برای سوئیچ کردن صفحات داخل center

    @FXML
    private Button myDivarButton; // تزریق دکمه از فایل FXML

    private Node homeView; // 🏠 ذخیره پوسته صفحه اصلی (لیست آگهی‌ها) برای بازگشت مجدد

    private final ContextMenu hoverMenu = new ContextMenu();
    private final HttpClient client = HttpClient.newHttpClient();

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
     * 🏠 متد عمومی برای بازگرداندن صفحه اصلی (لیست آگهی‌ها) به بخش مرکزی
     */
    public void showHomeScreen() {
        Platform.runLater(() -> {
            if (homeView != null) {
                mainBorderPane.setCenter(homeView);
                System.out.println("🔄 کانتینر مرکزی به لیست آگهی‌های اصلی بازگشت.");
            }
        });
    }

    /**
     * 📝 ۱. بارگذاری فرم ثبت آگهی جدید و متصل کردن ارجاع کنترلر
     */
    @FXML
    private void onRegisterAdClick() {
        try {
            // ۱. ذخیره پوسته فعلی مرکز صفحه (لیست آگهی‌ها) قبل از تعویض آن
            if (homeView == null) {
                homeView = mainBorderPane.getCenter();
            }

            // بارگذاری فایل FXML فرم ثبت آگهی
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register-ad-view.fxml"));
            VBox registerForm = loader.load();

            // ۲. تزریق ارجاع این کنترلر (this) به RegisterAdController جهت هدایت پس از ثبت موفق
            RegisterAdController registerAdController = loader.getController();
            registerAdController.setHelloController(this);

            // قرار دادن فرم طراحی شده در بخش مرکزی BorderPane
            mainBorderPane.setCenter(registerForm);
            System.out.println("🔄 فرم ثبت آگهی با موفقیت در لایوت مرکزی رندر شد.");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("خطا در بارگذاری", "مشکلی در باز کردن فرم ثبت آگهی رخ داده است: " + e.getMessage());
        }
    }

    /**
     * 📋 ۲. دریافت آگهی‌های اختصاصی خود کاربر (GET /my)
     */
    @FXML
    private void onLoadMyAdsClick() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/my"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (handleUnauthorized(response.statusCode())) return;

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            System.out.println("آگهی‌های من از سرور: " + response.body());
                            showSuccessAlert("بارگذاری موفق", "آگهی‌های شما با موفقیت دریافت شد.");
                        } else {
                            showErrorAlert("خطا", "عدم امکان بارگذاری آگهی‌ها. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    /**
     * ✏️ ۳. ویرایش آگهی (PUT /advertisements/{id})
     */
    private void onUpdateAdClick(Long adId, String newTitle, String newDesc, double newPrice) {
        String updatedJson = "{"
                + "\"title\": \"" + newTitle + "\","
                + "\"description\": \"" + newDesc + "\","
                + "\"price\": " + newPrice
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updatedJson))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    checkAndRefreshToken(response);
                    if (handleUnauthorized(response.statusCode())) return;

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showSuccessAlert("ویرایش موفق", "آگهی شما با موفقیت به‌روزرسانی شد.");
                        } else if (response.statusCode() == 403) {
                            showErrorAlert("خطای دسترسی", "شما مالک این آگهی نیستید و اجازه ویرایش آن را ندارید!");
                        } else {
                            showErrorAlert("خطا", "خطا در ویرایش آگهی. کد خطا: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    /**
     * 🗑️ ۴. حذف آگهی (DELETE /advertisements/{id})
     */
    private void onDeleteAdClick(Long adId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .DELETE()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    checkAndRefreshToken(response);
                    if (handleUnauthorized(response.statusCode())) return;

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showSuccessAlert("حذف موفق", "آگهی مورد نظر با موفقیت حذف شد.");
                        } else if (response.statusCode() == 403) {
                            showErrorAlert("خطای دسترسی", "شما اجازه حذف این آگهی را ندارید!");
                        } else {
                            showErrorAlert("خطا", "حذف آگهی با خطا مواجه شد. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    // ==========================================
    // 🛠️ متدهای کمکی و ماژولار برنامه برای مدیریت توکن و خطاها
    // ==========================================

    /**
     * 🔄 استخراج و تمدید خودکار توکن از هدر پاسخ (Sliding Expiration)
     */
    private void checkAndRefreshToken(HttpResponse<?> response) {
        response.headers().firstValue("Authorization").ifPresent(authHeader -> {
            if (authHeader.startsWith("Bearer ")) {
                String newToken = authHeader.substring(7);
                MainApplication.jwtToken = newToken;
                System.out.println("🔄 [JavaFX] توکن تازه‌نفس دریافت و تمدید شد.");
            }
        });
    }

    /**
     * 🛡️ مچ‌گیری از توکن‌های منقضی شده (خطای 401) و ریدایرکت فوری به صفحه لاگین
     */
    private boolean handleUnauthorized(int statusCode) {
        if (statusCode == 401) {
            Platform.runLater(() -> {
                Stage currentStage = (Stage) myDivarButton.getScene().getWindow();
                MainApplication.redirectToLogin(currentStage, "نشست شما به پایان رسیده است. لطفاً مجدداً وارد شوید.");
            });
            return true;
        }
        return false;
    }

    /**
     * 🌐 مدیریت خطاهای شبکه و قطع اتصال
     */
    private Void handleNetworkException(Throwable e) {
        Platform.runLater(() -> showErrorAlert("خطا در اتصال", "خطا در ارتباط با سرور: " + e.getMessage()));
        return null;
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}