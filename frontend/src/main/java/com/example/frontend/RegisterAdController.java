package com.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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

    // 🏙️ فیلدهای جدید برای شهر و دسته‌بندی
    @FXML
    private ComboBox<String> cityInput;

    @FXML
    private ComboBox<String> categoryInput;

    // 🌟 ارجاع به کنترلر اصلی برای مدیریت جابجایی صفحات
    private HelloController helloController;

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * 🔄 متد لایف‌سایکل جاوااف‌ایکس برای مقداردهی اولیه گزینه‌های ComboBox
     */
    @FXML
    public void initialize() {
        // پر کردن لیست شهرهای بزرگ ایران
        cityInput.getItems().addAll(
                "تهران", "مشهد", "اصفهان", "شیراز", "تبریز",
                "کرج", "اهواز", "قم", "کرمانشاه", "ارومیه", "رشت"
        );

        // پر کردن لیست دسته‌بندی‌های استاندارد بازارچه
        categoryInput.getItems().addAll(
                "کالای دیجیتال", "وسایل نقلیه", "املاک",
                "لوازم خانگی", "مد و پوشاک", "سرگرمی و فراغت", "خدمات"
        );
    }

    /**
     * 🔄 متد Setter برای تزریق کنترلر اصلی از طرف HelloController
     */
    public void setHelloController(HelloController helloController) {
        this.helloController = helloController;
    }

    /**
     * 🚀 تأیید و ارسال آگهی به سرور به همراه شهر و دسته‌بندی
     */
    @FXML
    private void onSubmitAdClick() {
        String title = titleInput.getText().trim();
        String priceText = priceInput.getText().trim();
        String description = descriptionInput.getText().trim();

        // 🟢 دریافت مقادیر انتخاب شده از کمبوباکس‌ها
        String selectedCity = cityInput.getValue();
        String selectedCategory = categoryInput.getValue();

        // ۱. اعتبارسنجی فیلدها (شامل شهر و دسته‌بندی)
        if (title.isEmpty() || priceText.isEmpty() || description.isEmpty() || selectedCity == null || selectedCategory == null) {
            showAlert(Alert.AlertType.ERROR, "خطای ورودی", "لطفاً تمامی فیلدها از جمله شهر و دسته‌بندی را تکمیل کنید.");
            return;
        }

        try {
            Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "خطای ورودی", "لطفاً برای قیمت فقط عدد انگلیسی وارد کنید.");
            return;
        }

        // 🛡️ ایمن‌سازی متون ورودی برای جلوگیری از خراب شدن ساختار JSON
        String safeTitle = escapeJson(title);
        String safeDescription = escapeJson(description);
        String safeCity = escapeJson(selectedCity);
        String safeCategory = escapeJson(selectedCategory);

        // ۲. ساخت بدنه پکت داده JSON به صورت کاملاً امن شامل اطلاعات جدید
        String jsonBody = "{"
                + "\"title\": \"" + safeTitle + "\","
                + "\"description\": \"" + safeDescription + "\","
                + "\"price\": " + priceText + ","
                + "\"city\": \"" + safeCity + "\","
                + "\"category\": \"" + safeCategory + "\""
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
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "آگهی شما با موفقیت ثبت شد!");

                            // 🧹 پاک‌سازی کامل فیلدهای فرم
                            clearFormFields();

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
        clearFormFields();

        // 🚀 بازگشت به لایوت قبلی بدون هیچ تعاملی با سرور
        if (helloController != null) {
            helloController.showHomeScreen();
            System.out.println("❌ کاربر از ثبت آگهی منصرف شد.");
        }
    }

    /**
     * 🧹 متد کمکی برای پاک‌سازی فیلدهای متنی و بازنشانی کمبوباکس‌ها
     */
    private void clearFormFields() {
        titleInput.clear();
        priceInput.clear();
        descriptionInput.clear();
        cityInput.getSelectionModel().clearSelection();
        categoryInput.getSelectionModel().clearSelection();
    }

    /**
     * 🧼 متد کمکی برای خنثی‌سازی کاراکترهای مخرب در فرآیند ساخت دستی JSON
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}