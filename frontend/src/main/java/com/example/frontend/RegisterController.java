package com.example.frontend;

import com.example.frontend.service.HttpService; // کلاس کمکی که در مرحله قبل ساختیم
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    // نگاشت دقیق فیلدها با fx:id های داخل FXML
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    /**
     * عملیات کلیک روی دکمه "ثبت‌نام و ایجاد حساب"
     */
    @FXML
    void onRegisterSubmitClick(ActionEvent event) {
        // ۱. دریافت مقادیر ورودی
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // ۲. اعتبارسنجی خالی نبودن فیلدها
        if (name.isEmpty() || username.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "خطای ورودی", "لطفاً تمامی فیلدها را تکمیل کنید.");
            return;
        }

        // بررسی انگلیسی بودن و فرمت نام کاربری
        String usernamePattern = "^[a-zA-Z0-9_]{3,20}$";
        if (!username.matches(usernamePattern)) {
            showAlert(Alert.AlertType.WARNING, "فرمت اشتباه نام کاربری",
                    "نام کاربری باید فقط شامل حروف انگلیسی، اعداد یا خط تیره (_) باشد.\nتعداد کاراکتر مجاز: ۳ تا ۲۰ حرف.");
            return;
        }

        // ۳. بررسی فرمت شماره تماس (فرمت استاندارد ایران: شروع با 09 و دقیقاً 11 رقم)
        String phonePattern = "^09\\d{9}$";
        if (!phone.matches(phonePattern)) {
            showAlert(Alert.AlertType.WARNING, "فرمت اشتباه شماره تماس",
                    "شماره تماس وارد شده معتبر نیست!\nباید با 09 شروع شده و شامل 11 رقم باشد. (مثال: 09123456789)");
            return;
        }

        // ۴. بررسی فرمت ایمیل (وجود علامت @ و دامین معتبر)
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        if (!email.matches(emailPattern)) {
            showAlert(Alert.AlertType.WARNING, "فرمت اشتباه ایمیل",
                    "ایمیل وارد شده معتبر نیست!\nفرمت صحیح: example@domain.com");
            return;
        }

        // ۵. بررسی حداقل طول رمز عبور (اختیاری اما برای امنیت بیشتر مفید است)
        if (password.length() < 4) {
            showAlert(Alert.AlertType.WARNING, "رمز عبور ضعیف", "رمز عبور باید حداقل 4 کاراکتر باشد.");
            return;
        }

        // ۶. ساخت ساختار JSON و ارسال به اسپرینگ بوت (در صورت تایید تمام فرمت‌ها)
        String jsonBody = String.format(
                "{\"name\":\"%s\",\"username\":\"%s\",\"phoneNumber\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                name, username, phone, email, password
        );

        HttpService.sendPost("/register", jsonBody)
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 201) {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "ثبت‌نام شما با موفقیت انجام شد.");
                            navigateToLogin(event);
                        } else if (response.statusCode() == 409) {
                            showAlert(Alert.AlertType.ERROR, "خطا در ثبت‌نام", response.body());
                        } else {
                            showAlert(Alert.AlertType.ERROR, "خطای سرور", "مشکلی در پردازش درخواست رخ داده است.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "خطای شبکه", "ارتباط با سرور برقرار نشد!")
                    );
                    return null;
                });
    }
    /**
     * دکمه جابه‌جایی و بازگشت به صفحه ورود
     */
    @FXML
    void onGoToLoginClick(ActionEvent event) {
        navigateToLogin(event);
    }

    /**
     * دکمه انصراف (پاک کردن فیلدها یا بستن پنجره)
     */
    @FXML
    void onCancelClick(ActionEvent event) {
        nameField.clear();
        usernameField.clear();
        phoneField.clear();
        emailField.clear();
        passwordField.clear();
    }

    /**
     * متد کمکی برای ناوبری و سوئیچ به صفحه لاگین
     */
    private void navigateToLogin(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            // ابعاد استاندارد متناسب با صفحه لاگین مدرن شما (مثلاً 440 در 580)
            Scene scene = new Scene(fxmlLoader.load(), 440, 580);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "خطا", "فایل login-view.fxml پیدا نشد.");
            e.printStackTrace();
        }
    }



    /**
     * متد کمکی برای نمایش پنجره‌های پاپ‌آپ (Alert)
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}