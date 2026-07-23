package com.example.frontend; // 📂 هماهنگ با پکیج جدید کنترلرها

import com.example.frontend.service.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private Button btnTogglePassword;

    /**
     * 🔗 هم‌گام‌سازی فیلد رمز مخفی و فیلد رمز نمایان (برای دکمه چشم)
     */
    @FXML
    private void initialize() {
        if (passwordVisibleField != null && passwordField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }
        restrictPasswordInput(passwordField);
        restrictPasswordInput(passwordVisibleField);
    }

    // 🚫 جلوگیری از تایپ فاصله و کاراکترهای غیرمجاز در فیلد رمز عبور (تایپ و paste هر دو فیلتر می‌شوند)
    private void restrictPasswordInput(javafx.scene.control.TextInputControl field) {
        if (field == null) return;
        field.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change ->
                change.getControlNewText().matches("[A-Za-z0-9!@#$%^&*_.\\-]*") ? change : null));
    }

    /**
     * 👁 نمایش یا مخفی کردن رمز عبور با دکمه چشم
     */
    @FXML
    private void onTogglePasswordClick() {
        boolean show = !passwordVisibleField.isVisible();
        passwordVisibleField.setVisible(show);
        passwordField.setVisible(!show);
        btnTogglePassword.setText(show ? "🙈" : "👁");
    }

    /**
     * عملیات کلیک روی دکمه ورود
     */
    @FXML
    private void onLoginSubmitClick() {
        String phone = phoneField.getText().trim();
        String password = passwordField.getText();

        if (phone.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "خطا", "لطفاً تمامی فیلدها را پر کنید.");
            return;
        }

        String jsonPayload = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", phone, password);

        HttpService.sendPost("/login", jsonPayload)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            // 🔑 استخراج توکن با متد هوشمند
                            String token = extractTokenFromJson(response.body());

                            // 👤 🟢 اصلاح شد: استخراج نام کاربری واقعی (مثل ali) از پاسخ سرور
                            String actualUsername = extractUsernameFromJson(response.body());

                            // 💾 ذخیره در سشن متمرکز MainApplication
                            MainApplication.jwtToken = token;

                            // اگر سرور یوزرنیم واقعی را فرستاده بود، آن را ذخیره کن؛ در غیر این صورت شماره تلفن را به عنوان پشتیبان بذار
                            MainApplication.currentUsername = (actualUsername != null) ? actualUsername : phone;

                            System.out.println("✅ توکن ذخیره شد: " + MainApplication.jwtToken);
                            System.out.println("👤 کاربر جاری سیستم: " + MainApplication.currentUsername);

                            closeWindow();
                            navigateToMain();
                            UiTheme.toast("خوش آمدید! ورود با موفقیت انجام شد.");

                        } else if (response.statusCode() == 401) {
                            String serverError = extractMessageFromJson(response.body());
                            showAlert(Alert.AlertType.ERROR, "خطا در ورود", serverError);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "خطای سرور", "مشکلی در سرور رخ داده است. وضعیت: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "خطای شبکه", "اتصال به سرور برقرار نشد. مطمئن شوید بک‌انند روشن است.")
                    );
                    return null;
                });
    }

    /**
     * جابه‌جایی به صفحه ثبت‌نام (اصلاح شده با مسیر مطلق)
     */
    @FXML
    private void onGoToRegisterClick() {
        try {
            closeWindow();

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/frontend/register-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage registerStage = new Stage();
            registerStage.setTitle("ثبت‌نام حساب کاربری");
            registerStage.initModality(Modality.APPLICATION_MODAL);
            registerStage.setScene(scene);
            registerStage.setResizable(true);
            registerStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در باز کردن صفحه ثبت‌نام!");
        }
    }

    /**
     * باز کردن صفحه اصلی برنامه (اصلاح شده با مسیر مطلق)
     */
    private void navigateToMain() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/frontend/hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage mainStage = new Stage();
            mainStage.setTitle("صفحه اصلی بازارچه دست دوم");
            mainStage.setScene(scene);
            mainStage.centerOnScreen();
            mainStage.show();

            mainStage.setOnCloseRequest(event -> {
                if (MainApplication.currentUsername != null) {
                    MainApplication.redirectToLogin(mainStage, "شما با موفقیت از حساب خود خارج شدید.");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "خطای بارگذاری", "خطا در باز کردن صفحه اصلی! فایل hello-view.fxml پیدا نشد.");
        }
    }

    private void closeWindow() {
        if (phoneField.getScene() != null) {
            Stage stage = (Stage) phoneField.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * 🧠 متد هوشمند استخراج پیام
     */
    private String extractMessageFromJson(String jsonBody) {
        try {
            if (jsonBody != null && jsonBody.contains("\"message\"")) {
                int keyIndex = jsonBody.indexOf("\"message\"");
                int colonIndex = jsonBody.indexOf(":", keyIndex);
                int quoteStart = jsonBody.indexOf("\"", colonIndex);
                int quoteEnd = jsonBody.indexOf("\"", quoteStart + 1);
                return jsonBody.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            // سایلنت
        }
        return "شماره موبایل یا رمز عبور اشتباه است.";
    }

    /**
     * 🔑 متد هوشمند استخراج توکن
     */
    private String extractTokenFromJson(String jsonBody) {
        try {
            if (jsonBody != null && jsonBody.contains("\"token\"")) {
                int keyIndex = jsonBody.indexOf("\"token\"");
                int colonIndex = jsonBody.indexOf(":", keyIndex);
                int quoteStart = jsonBody.indexOf("\"", colonIndex);
                int quoteEnd = jsonBody.indexOf("\"", quoteStart + 1);
                return jsonBody.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            System.out.println("❌ خطا در استخراج توکن ساختاریافته");
        }
        return null;
    }

    /**
     * 👤 🟢 اضافه شد: متد هوشمند استخراج نام‌کاربری واقعی از جی‌سون ورودی
     */
    private String extractUsernameFromJson(String jsonBody) {
        try {
            if (jsonBody != null && jsonBody.contains("\"username\"")) {
                int keyIndex = jsonBody.indexOf("\"username\"");
                int colonIndex = jsonBody.indexOf(":", keyIndex);
                int quoteStart = jsonBody.indexOf("\"", colonIndex);
                int quoteEnd = jsonBody.indexOf("\"", quoteStart + 1);
                return jsonBody.substring(quoteStart + 1, quoteEnd);
            }
        } catch (Exception e) {
            System.out.println("❌ خطا در استخراج نام کاربری واقعی از ساختار JSON");
        }
        return null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        UiTheme.styleAlert(alert);
        alert.showAndWait();
    }
}
