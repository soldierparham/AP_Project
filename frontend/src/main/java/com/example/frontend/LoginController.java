package com.example.frontend; // 📂 هماهنگ با پکیج جدید کنترلرها

import com.example.frontend.MainApplication;
import com.example.frontend.service.HttpService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;

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
                            // 🔑 استخراج توکن با متد هوشمند و جدید (مقاوم در برابر اسپیس)
                            String token = extractTokenFromJson(response.body());

                            // 💾 ذخیره در سشن متمرکز MainApplication
                            MainApplication.jwtToken = token;
                            MainApplication.currentUsername = phone;

                            System.out.println("✅ توکن با موفقیت در فرانت ذخیره شد: " + MainApplication.jwtToken);

                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "خوش آمدید! ورود با موفقیت انجام شد.");

                            closeWindow();
                            navigateToMain();

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

    @FXML
    private void onCancelClick() {
        closeWindow();
    }

    /**
     * جابه‌جایی به صفحه ثبت‌نام (اصلاح شده با مسیر مطلق)
     */
    @FXML
    private void onGoToRegisterClick() {
        try {
            closeWindow();

            // 🌟 اصلاح تله اول: استفاده از مسیر مطلق برای پیدا کردن FXML
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
            // 🌟 اصلاح تله اول: استفاده از مسیر مطلق برای پیدا کردن FXML
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/example/frontend/hello-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            Stage mainStage = new Stage();
            mainStage.setTitle("صفحه اصلی بازارچه دست دوم");
            mainStage.setScene(scene);
            mainStage.centerOnScreen();
            mainStage.show();

            // مدیریت دکمه ضربدر پنجره اصلی (ارتباط با متد لاگ‌اوت متمرکز شما)
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
     * 🧠 متد هوشمند استخراج پیام (بدون حساسیت به فاصله و اسپیس‌های تعمدی JSON)
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
     * 🔑 متد هوشمند استخراج توکن (تضمین حل تله دوم و نادیده گرفتن فضاهای خالی)
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        String backgroundColor = "#1e1e2a";
        String textColor = "#ffffff";
        String borderColor = (type == Alert.AlertType.ERROR || type == Alert.AlertType.WARNING) ? "#ff5555" : "#50fa7b";
        String buttonColor = (type == Alert.AlertType.ERROR || type == Alert.AlertType.WARNING) ? "#ff5555" : "#50fa7b";

        dialogPane.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 2px;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 15px;"
        );

        javafx.scene.Node contentLabel = dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle(
                    "-fx-text-fill: " + textColor + ";" +
                            "-fx-font-family: 'Segoe UI', 'Vazirmatn';" +
                            "-fx-font-size: 14px;"
            );
        }

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: " + buttonColor + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 5px;" +
                            "-fx-padding: 6px 20px;"
            );
        }

        alert.showAndWait();
    }
}