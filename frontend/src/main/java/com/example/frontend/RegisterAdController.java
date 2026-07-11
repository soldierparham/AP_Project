package com.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterAdController {

    @FXML
    private TextField titleInput;

    @FXML
    private TextField priceInput;

    @FXML
    private TextArea descriptionInput;

    // 🌟 ارجاع به کنترلر اصلی برای مدیریت جابجایی صفحات
    private HelloController helloController;

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * 🔄 متد Setter برای تزریق کنترلر اصلی از طرف HelloController
     */
    public void setHelloController(HelloController helloController) {
        this.helloController = helloController;
    }

    /**
     * 🚀 تأیید و ارسال آگهی به سرور
     */
    @FXML
    private void onSubmitAdClick() {
        String title = titleInput.getText().trim();
        String priceText = priceInput.getText().trim();
        String description = descriptionInput.getText().trim();

        // ۱. اعتبارسنجی فیلدها
        if (title.isEmpty() || priceText.isEmpty() || description.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطای ورودی", "لطفاً تمامی فیلدها را تکمیل کنید.");
            return;
        }

        try {
            Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "خطای ورودی", "لطفاً برای قیمت فقط عدد انگلیسی وارد کنید.");
            return;
        }

        // 🛡️ اصلاح کلیدی: ایمن‌سازی متون ورودی برای جلوگیری از خراب شدن ساختار JSON
        String safeTitle = escapeJson(title);
        String safeDescription = escapeJson(description);

        // ۲. ساخت بدنه پکت داده JSON به صورت کاملاً امن
        String jsonBody = "{"
                + "\"title\": \"" + safeTitle + "\","
                + "\"description\": \"" + safeDescription + "\","
                + "\"price\": " + priceText
                + "}";

        // ۳. ارسال درخواست به بک‌انند با توکن فعال
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HelloController.BASE_URL + "/api/advertisements"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // تمدید توکن در صورت وجود هدر جدید
                    response.headers().firstValue("Authorization").ifPresent(authHeader -> {
                        if (authHeader.startsWith("Bearer ")) {
                            MainApplication.jwtToken = authHeader.substring(7);
                            System.out.println("🔄 توکن فرانت‌انند پس از ثبت موفق آگهی تمدید شد.");
                        }
                    });

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "آگهی شما با موفقیت ثبت شد!");

                            // 🧹 پاک‌سازی فیلدهای فرم
                            titleInput.clear();
                            priceInput.clear();
                            descriptionInput.clear();

                            // هدایت خودکار و آنی کاربر به صفحه اصلی
                            if (helloController != null) {
                                helloController.showHomeScreen();
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "خطای سرور", "خطا در ثبت آگهی. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "عدم اتصال به سرور: " + e.getMessage()));
                    return null;
                });
    }

    /**
     * ❌ انصراف از ثبت آگهی و بازگشت آنی به صفحه اصلی (لیست آگهی‌ها)
     */
    @FXML
    private void onCancelClick() {
        // 🧹 پاک‌سازی فیلدهای فرم برای مراجعات بعدی
        titleInput.clear();
        priceInput.clear();
        descriptionInput.clear();

        // 🚀 بازگشت به لایوت قبلی بدون هیچ تعاملی با سرور
        if (helloController != null) {
            helloController.showHomeScreen();
            System.out.println("❌ کاربر از ثبت آگهی منصرف شد.");
        }
    }

    /**
     * 🧼 متد کمکی برای خنثی‌سازی کاراکترهای مخرب در فرآیند ساخت دستی JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")   // خنثی کردن بک‌اسلش
                .replace("\"", "\\\"")   // خنثی کردن گیومه
                .replace("\n", "\\n")    // تبدیل اینتر به کاراکتر مجاز n\
                .replace("\r", "");      // حذف کاراکترهای بازگشت هدر
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        // استفاده از showAndWait پایداری بهتری در انتقال فوکوس ایجاد می‌کند
        alert.showAndWait();
    }
}