package com.example.frontend;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RegisterAdController {

    @FXML
    private TextField titleInput;

    @FXML
    private TextField priceInput;

    @FXML
    private TextArea descriptionInput;

    @FXML
    private ComboBox<String> cityInput;

    @FXML
    private ComboBox<String> categoryInput;

    // 🖼️ فیلد جدید برای نمایش مسیر یا نام عکس انتخاب شده در رابط کاربری
    @FXML
    private Label imagePathLabel;

    // متغیر محلی برای نگهداری فایل عکس انتخاب شده
    private File selectedImageFile;

    private HelloController helloController;
    private final HttpClient client = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        cityInput.getItems().addAll(
                "تهران", "مشهد", "اصفهان", "شیراز", "تبریز",
                "کرج", "اهواز", "قم", "کرمانشاه", "ارومیه", "رشت"
        );

        categoryInput.getItems().addAll(
                "کالای دیجیتال", "وسایل نقلیه", "املاک",
                "لوازم خانگی", "مد و پوشاک", "سرگرمی و فراغت", "خدمات"
        );

        if (imagePathLabel != null) {
            imagePathLabel.setText("هیچ عکسی انتخاب نشده است.");
        }
    }

    public void setHelloController(HelloController helloController) {
        this.helloController = helloController;
    }

    /**
     * 📸 ۱. باز کردن پنجره انتخاب فایل برای عکس آگهی
     */
    @FXML
    private void onSelectImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("انتخاب عکس آگهی");

        // فقط فایل‌های تصویری مجاز باشند
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("تصاویر", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) titleInput.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            if (imagePathLabel != null) {
                imagePathLabel.setText("عکس انتخاب شد: " + file.getName());
            }
            System.out.println("🔍 [DEBUG] عکس انتخاب شده: " + file.getAbsolutePath());
        }
    }

    /**
     * 🚀 ۲. تأیید و ارسال اطلاعات به همراه عکس به سرور
     */
    @FXML
    private void onSubmitAdClick() {
        String title = titleInput.getText().trim();
        String priceText = priceInput.getText().trim();
        String description = descriptionInput.getText().trim();
        String selectedCity = cityInput.getValue();
        String selectedCategory = categoryInput.getValue();

        // 🚨 لاگ وضعیت برای خطایابی سریع
        System.out.println("====== 🔍 تست وضعیت دکمه ثبت ======");
        System.out.println("📸 وضعیت فایل انتخابی: " + (selectedImageFile == null ? "NULL (خالی)" : selectedImageFile.getAbsolutePath()));
        if (imagePathLabel != null) {
            System.out.println("📝 متن لیبل عکس: " + imagePathLabel.getText());
        }

        // اعتبارسنجی اولیه فرم
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

        // 🔄 بررسی دقیق‌تر وضعیت عکس
        if (selectedImageFile != null && selectedImageFile.exists()) {
            System.out.println("🚀 [OK] فایل پیدا شد. شروع فرآیند آپلود ناهمگام...");

            uploadImageAsync(selectedImageFile)
                    .thenAccept(imageUrl -> {
                        System.out.println("💾 [SUCCESS] دریافت آدرس از سرور: " + imageUrl);
                        submitAdvertisement(title, priceText, description, selectedCity, selectedCategory, imageUrl);
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            System.err.println("❌ خطا در جریان آپلود: " + cause.getMessage());
                            showAlert(Alert.AlertType.ERROR, "خطای آپلود تصویر",
                                    "آپلود عکس با خطا مواجه شد.\nجزئیات: " + cause.getMessage());
                        });
                        return null;
                    });
        } else {
            System.out.println("⚠️ [WARNING] برنامه‌ فایلی پیدا نکرد یا selectedImageFile نال است. ارسال بدون عکس...");
            submitAdvertisement(title, priceText, description, selectedCity, selectedCategory, null);
        }
    }

    /**
     * 📡 ۳. متد کمکی برای ارسال ناهمگام فایل تصویر (Multipart Request)
     */
    private CompletableFuture<String> uploadImageAsync(File file) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String boundary = "JavaFX-Boundary-" + UUID.randomUUID().toString();
            byte[] multipartBody = createMultipartBody(file, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HelloController.BASE_URL + "/upload"))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        String responseBody = response.body();
                        System.out.println("📥 [DEBUG] پاسخ خام سرور بعد از آپلود عکس: " + responseBody);

                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            // استخراج منعطف آدرس عکس از پاسخ جیسون
                            String imageUrl = extractImageUrlFromJson(responseBody);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                System.out.println("✅ [DEBUG] آپلود عکس موفقیت‌آمیز بود. مسیر استخراج شده: " + imageUrl);
                                future.complete(imageUrl);
                            } else {
                                future.completeExceptionally(new RuntimeException("آدرس تصویر در پاسخ JSON یافت نشد. پاسخ: " + responseBody));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException("کد وضعیت سرور: " + response.statusCode() + " - پاسخ: " + responseBody));
                        }
                    })
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    /**
     * 📝 ۴. ثبت نهایی آگهی در دیتابیس به همراه آدرس عکس دریافت شده
     */
    private void submitAdvertisement(String title, String priceText, String description, String city, String category, String imageUrl) {
        String safeTitle = escapeJson(title);
        String safeDescription = escapeJson(description);
        String safeCity = escapeJson(city);
        String safeCategory = escapeJson(category);
        String safeImageUrl = imageUrl != null ? escapeJson(imageUrl) : "";

        // ✨ تکنیک اطمینان: ارسال همزمان image_url و imageUrl تا اسپرینگ جفتش را بررسی کند
        String jsonBody = "{"
                + "\"title\": \"" + safeTitle + "\","
                + "\"description\": \"" + safeDescription + "\","
                + "\"price\": " + priceText + ","
                + "\"city\": \"" + safeCity + "\","
                + "\"category\": \"" + safeCategory + "\","
                + "\"image_url\": \"" + safeImageUrl + "\","
                + "\"imageUrl\": \"" + safeImageUrl + "\""
                + "}";

        System.out.println("📤 [DEBUG] در حال ارسال JSON ثبت آگهی به بک‌اند: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HelloController.BASE_URL + "/api/advertisements"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("📥 [DEBUG] پاسخ سرور پس از ثبت نهایی آگهی: Status=" + response.statusCode());

                    response.headers().firstValue("Authorization").ifPresent(authHeader -> {
                        if (authHeader.startsWith("Bearer ")) {
                            MainApplication.jwtToken = authHeader.substring(7);
                            System.out.println("🔄 توکن فرانت‌انند پس از ثبت موفق آگهی تمدید شد.");
                        }
                    });

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "آگهی شما با موفقیت ثبت شد!");
                            clearFormFields();
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
     * ⚙️ ۵. متد کمکی ساخت پکت استاندارد Multipart/Form-Data به صورت بایتی
     */
    private byte[] createMultipartBody(File file, String boundary) throws IOException {
        String fileName = file.getName();
        String mimeType = "image/jpeg";
        try {
            mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) mimeType = "image/jpeg";
        } catch (Exception e) {
            // پیش‌فرض jpeg
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
        sb.append("Content-Type: ").append(mimeType).append("\r\n\r\n");

        byte[] header = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        byte[] body = new byte[header.length + fileBytes.length + footer.length];
        System.arraycopy(header, 0, body, 0, header.length);
        System.arraycopy(fileBytes, 0, body, header.length, fileBytes.length);
        System.arraycopy(footer, 0, body, header.length + fileBytes.length, footer.length);

        return body;
    }

    /**
     * 🔍 ۶. متد کمکی اصلاح‌شده برای استخراج آدرس تصویر با پشتیبانی دقیق از کلیدهای دیتابیس
     */
    private String extractImageUrlFromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;

        if (!json.contains("{") && json.contains("/")) {
            return json.trim();
        }

        // 🛠️ اصلاح کلید اصلی به "image_url" با کاراکتر آندراسکور (_)
        String[] keys = {"\"image_url\"", "\"imageUrl\"", "\"image-url\"", "\"url\"", "\"fileUrl\"", "\"path\""};
        for (String key : keys) {
            int index = json.indexOf(key);
            if (index != -1) {
                int colonIndex = json.indexOf(":", index);
                if (colonIndex != -1) {
                    int startQuote = json.indexOf("\"", colonIndex + 1);
                    if (startQuote != -1) {
                        int endQuote = json.indexOf("\"", startQuote + 1);
                        if (endQuote != -1) {
                            return json.substring(startQuote + 1, endQuote);
                        }
                    }
                }
            }
        }
        return null;
    }

    @FXML
    private void onCancelClick() {
        clearFormFields();
        if (helloController != null) {
            helloController.showHomeScreen();
            System.out.println("❌ کاربر از ثبت آگهی منصرف شد.");
        }
    }

    private void clearFormFields() {
        titleInput.clear();
        priceInput.clear();
        descriptionInput.clear();
        cityInput.getSelectionModel().clearSelection();
        categoryInput.getSelectionModel().clearSelection();
        selectedImageFile = null;
        if (imagePathLabel != null) {
            imagePathLabel.setText("هیچ عکسی انتخاب نشده است.");
        }
    }

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