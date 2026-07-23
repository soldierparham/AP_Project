package com.example.frontend;

import javafx.application.Platform;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 📝 کنترلر ثبت آگهی — نسخه تکمیل‌شده:
 * 🖼️ پشتیبانی از انتخاب و آپلود چند عکس به صورت همزمان (گالری تصاویر — بخش امتیازی سند پروژه)
 * آدرس عکس‌ها با کاما از هم جدا ذخیره می‌شوند (مثل /uploads/a.jpg,/uploads/b.jpg)
 */
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

    // کمبوی زیردسته (فقط وقتی دسته اصلی انتخابی، زیردسته داشته باشد نمایش داده می‌شود)
    @FXML
    private ComboBox<String> subCategoryInput;

    // نگاشت دسته اصلی ← زیردسته‌ها
    private final java.util.Map<String, java.util.List<String>> subcategoriesOf = new java.util.LinkedHashMap<>();

    // 🖼️ FlowPane نمایش thumbnail عکس‌های انتخاب‌شده
    @FXML
    private FlowPane imageThumbsPane;

    // 🖼️ لیست عکس‌های انتخاب‌شده (به جای تک‌فایل قبلی)
    private final List<File> selectedImageFiles = new ArrayList<>();

    // ⭐ ایندکس عکس اصلی در selectedImageFiles — مشابه منطق mainIdx در بخش ویرایش آگهی (HelloController)
    private int mainIdx = 0;

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

        // 🎨 هماهنگ‌سازی رنگ کمبوباکس‌ها با تم برنامه
        HelloController.styleComboBox(cityInput);
        HelloController.styleComboBox(categoryInput);
        if (subCategoryInput != null) {
            HelloController.styleComboBox(subCategoryInput);
            // با تغییر دسته اصلی، لیست زیردسته‌ها به‌روز می‌شود
            categoryInput.valueProperty().addListener((obs, oldV, newV) -> refreshSubCategoryCombo(newV));
        }

        // 🗂️ دریافت دسته‌بندی‌های به‌روز از سرور (شامل دسته‌های ادمین)
        loadCategoriesFromServer();

        // دریافت شهرهای به‌روز از سرور (شامل شهرهای اضافه/ویرایش‌شده توسط ادمین)
        loadCitiesFromServer();

        // thumbnails از onSelectImageClick بارگذاری می‌شوند
    }

    // 🗂 دریافت دسته‌بندی‌ها از سرور: فقط دسته‌های اصلی در لیست اول؛ زیردسته‌ها در لیست دوم
    private void loadCategoriesFromServer() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HelloController.BASE_URL + "/api/categories"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;
                    // تجزیه هر دسته همراه با شناسه و والد
                    java.util.List<Long> ids = new java.util.ArrayList<>();
                    java.util.List<String> names = new java.util.ArrayList<>();
                    java.util.List<Long> parents = new java.util.ArrayList<>();
                    java.util.regex.Matcher obj = java.util.regex.Pattern.compile("\\{[^{}]*\\}").matcher(response.body());
                    while (obj.find()) {
                        String o = obj.group();
                        java.util.regex.Matcher mId = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(o);
                        java.util.regex.Matcher mName = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(o);
                        java.util.regex.Matcher mParent = java.util.regex.Pattern.compile("\"parentId\"\\s*:\\s*(\\d+)").matcher(o);
                        if (!mId.find() || !mName.find()) continue;
                        ids.add(Long.parseLong(mId.group(1)));
                        names.add(mName.group(1).trim());
                        parents.add(mParent.find() ? Long.parseLong(mParent.group(1)) : -1L);
                    }
                    if (names.isEmpty()) return;
                    // ساخت درخت: دسته‌های اصلی و زیردسته‌های هر کدام
                    java.util.List<String> mains = new java.util.ArrayList<>();
                    java.util.Map<String, java.util.List<String>> tree = new java.util.LinkedHashMap<>();
                    for (int i = 0; i < names.size(); i++) {
                        if (parents.get(i) >= 0 && ids.contains(parents.get(i))) continue; // زیردسته است
                        String parentName = names.get(i);
                        if (parentName.isEmpty() || mains.contains(parentName)) continue;
                        mains.add(parentName);
                        java.util.List<String> kids = new java.util.ArrayList<>();
                        long pid = ids.get(i);
                        for (int j = 0; j < names.size(); j++) {
                            if (parents.get(j) == pid && !names.get(j).isEmpty() && !kids.contains(names.get(j))) {
                                kids.add(names.get(j));
                            }
                        }
                        tree.put(parentName, kids);
                    }
                    if (mains.isEmpty()) return;
                    javafx.application.Platform.runLater(() -> {
                        subcategoriesOf.clear();
                        subcategoriesOf.putAll(tree);
                        categoryInput.getItems().setAll(mains);
                        refreshSubCategoryCombo(categoryInput.getValue());
                    });
                });
    }

    /** نمایش/مخفی‌سازی کمبوی زیردسته بر اساس دسته اصلی انتخاب‌شده */
    private void refreshSubCategoryCombo(String mainCategory) {
        if (subCategoryInput == null) return;
        java.util.List<String> kids = mainCategory == null ? null : subcategoriesOf.get(mainCategory);
        boolean has = kids != null && !kids.isEmpty();
        subCategoryInput.getItems().setAll(has ? kids : java.util.Collections.<String>emptyList());
        subCategoryInput.getSelectionModel().clearSelection();
        subCategoryInput.setVisible(has);
        subCategoryInput.setManaged(has);
    }

    // دریافت لیست شهرها از سرور تا شهرهای جدید ادمین هم قابل انتخاب باشند
    private void loadCitiesFromServer() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HelloController.BASE_URL + "/api/cities"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;
                    java.util.List<String> names = new java.util.ArrayList<>();
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("\"name\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(response.body());
                    while (m.find()) {
                        String n = m.group(1).trim();
                        if (!n.isEmpty() && !names.contains(n)) names.add(n);
                    }
                    if (names.isEmpty()) return;
                    javafx.application.Platform.runLater(() ->
                            cityInput.getItems().setAll(names));
                });
    }

    public void setHelloController(HelloController helloController) {
        this.helloController = helloController;
    }

    /**
     * 📸 ۱. باز کردن پنجره انتخاب چند فایل برای عکس‌های آگهی
     * (با Ctrl یا Shift می‌توانید چند عکس را همزمان انتخاب کنید)
     */
    @FXML
    private void onSelectImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("افزودن عکس (چندانتخابی با Ctrl)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("تصاویر", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) titleInput.getScene().getWindow();
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files != null && !files.isEmpty()) {
            selectedImageFiles.addAll(files); // add = تجمیعی است (مثل ویرایش)
            renderImageThumbs();
        }
    }

    /**
     * 🖼️ نمایش thumbnail هر عکس انتخاب‌شده.
     * دقیقاً همان الگو و طراحی بخش «ویرایش آگهی» (HelloController.buildImageThumb) است:
     * عکس اصلی کادر طلایی دارد و برای تعیین عکس دیگر به‌عنوان اصلی، دکمه ⭐ روی همان عکس زده می‌شود.
     * ترتیب واقعی لیست تا لحظهٔ ارسال تگییر نمی‌کند؛ فقط mainIdx ذخیره می‌شود و همین عکس
     * در لحظهٔ ارسال (onSubmitAdClick) به ابتدای لیست منتقل می‌شود.
     */
    private void renderImageThumbs() {
        if (imageThumbsPane == null) return;
        imageThumbsPane.getChildren().clear();
        int tot = selectedImageFiles.size();
        if (mainIdx >= tot && tot > 0) mainIdx = 0;
        for (int i = 0; i < selectedImageFiles.size(); i++) {
            final int idx = i;
            File file = selectedImageFiles.get(i);
            try {
                Image img = new Image(file.toURI().toString(), 140, 100, false, true, true);
                String tag = file.getName().length() > 14 ? file.getName().substring(0, 14) + "..." : file.getName();
                imageThumbsPane.getChildren().add(buildImageThumb(img, tag, idx == mainIdx,
                        () -> {
                            selectedImageFiles.remove(idx < selectedImageFiles.size() ? idx : selectedImageFiles.size() - 1);
                            if (mainIdx >= selectedImageFiles.size()) mainIdx = 0;
                            renderImageThumbs();
                        },
                        () -> { mainIdx = idx; renderImageThumbs(); }));
            } catch (Exception ignored) {}
        }
        if (selectedImageFiles.isEmpty()) {
            Label lbl = new Label("🖼️ هیچ عکسی انتخاب نشده");
            lbl.setStyle("-fx-text-fill: #8b7ca6; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");
            imageThumbsPane.getChildren().add(lbl);
        }
    }

    /** 🖼️ پیش‌نمایش کوچک عکس؛ عکس اصلی کادر طلایی دارد و بقیه دکمه ⭐ دارند. (عین پترن HelloController.buildImageThumb) */
    private VBox buildImageThumb(Image image, String tag, boolean isMain, Runnable onRemove, Runnable onSetMain) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(140); iv.setFitHeight(100); iv.setPreserveRatio(false);
        iv.setStyle("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.4),4,0,0,2);");

        Label lblTag = new Label(isMain ? "⭐ عکس اصلی" : tag);
        lblTag.setStyle(isMain
                ? "-fx-text-fill: #ffc83b; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn'; -fx-font-weight: bold;"
                : "-fx-text-fill: #b9a6df; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");

        Button btnR = new Button("🗑️ حذف");
        btnR.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");
        btnR.setOnAction(e -> onRemove.run());

        VBox box = new VBox(5, iv, lblTag, btnR);
        if (!isMain) {
            Button bm = new Button("⭐ عکس اصلی");
            bm.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px; -fx-font-family: 'Vazirmatn';");
            bm.setOnAction(e -> onSetMain.run());
            box.getChildren().add(bm);
        }
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #241942; -fx-border-color: " + (isMain ? "#ffc83b" : "#3b286b") + "; -fx-border-width: " + (isMain ? "2" : "1") + "; -fx-border-radius: 10; -fx-background-radius: 10;");
        return box;
    }

    /**
     * 🚀 ۲. تأیید و ارسال اطلاعات به همراه عکس‌ها به سرور
     */
    @FXML
    private void onSubmitAdClick() {
        String title = titleInput.getText().trim();
        String priceText = priceInput.getText().trim();
        String description = descriptionInput.getText().trim();
        String selectedCity = cityInput.getValue();
        String mainCategory = categoryInput.getValue();
        // اگر زیردسته انتخاب شده باشد، همان به عنوان دسته آگهی ثبت می‌شود
        String selectedCategory = (subCategoryInput != null && subCategoryInput.isVisible() && subCategoryInput.getValue() != null)
                ? subCategoryInput.getValue() : mainCategory;

        // 🚨 لاگ وضعیت برای خطایابی سریع
        System.out.println("====== 🔍 تست وضعیت دکمه ثبت ======");
        System.out.println("📸 تعداد عکس‌های انتخابی: " + selectedImageFiles.size());

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

        // 🔄 ��پلود چند عکس به صورت همزمان در یک درخواست
        // ⭐ عکس اصلی انتخاب‌شده (mainIdx) را به ابتدای لیست منتقل می‌کنیم — عین منطق بخش ویرایش آگهی (HelloController)
        List<File> orderedFiles = new ArrayList<>(selectedImageFiles);
        if (mainIdx > 0 && mainIdx < orderedFiles.size()) {
            orderedFiles.add(0, orderedFiles.remove(mainIdx));
        }

        List<File> validFiles = new ArrayList<>();
        for (File f : orderedFiles) {
            if (f != null && f.exists()) validFiles.add(f);
        }

        if (!validFiles.isEmpty()) {
            System.out.println("🚀 [OK] " + validFiles.size() + " فایل پیدا شد. شروع فرآیند آپلود ناهمگام...");

            uploadImagesAsync(validFiles)
                    .thenAccept(imageUrl -> {
                        System.out.println("💾 [SUCCESS] دریافت آدرس(ها) از سرور: " + imageUrl);
                        submitAdvertisement(title, priceText, description, selectedCity, selectedCategory, imageUrl);
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            System.err.println("❌ خطا در جریان آپلود: " + cause.getMessage());
                            showAlert(Alert.AlertType.ERROR, "خطای آپلود تصویر",
                                    "آپلود عکس(ها) با خطا مواجه شد.\nجزئیات: " + cause.getMessage());
                        });
                        return null;
                    });
        } else {
            System.out.println("⚠️ [WARNING] هیچ عکسی انتخاب نشده یا فایل‌ها موجود نیستند. ارسال بدون عکس...");
            submitAdvertisement(title, priceText, description, selectedCity, selectedCategory, null);
        }
    }

    /**
     * 📡 ۳. ارسال ناهمگام چند فایل تصویر در یک درخواست Multipart
     * (هر فایل با کلید "files" ارسال می‌شود — بک‌اند هر دو کلید file و files را می‌پذیرد)
     */
    private CompletableFuture<String> uploadImagesAsync(List<File> files) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String boundary = "JavaFX-Boundary-" + UUID.randomUUID();
            byte[] multipartBody = createMultipartBody(files, boundary);

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
                            String imageUrl = extractImageUrlFromJson(responseBody);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                System.out.println("✅ [DEBUG] آپلود عکس(ها) موفقیت‌آمیز بود. مسیر(ها): " + imageUrl);
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
     * 📝 ۴. ثبت نهایی آگهی در دیتابیس به همراه آدرس عکس(ها)ی دریافت شده
     */
    private void submitAdvertisement(String title, String priceText, String description, String city, String category, String imageUrl) {
        String safeTitle = escapeJson(title);
        String safeDescription = escapeJson(description);
        String safeCity = escapeJson(city);
        String safeCategory = escapeJson(category);
        String safeImageUrl = imageUrl != null ? escapeJson(imageUrl) : "";

        // ✨ ارسال همزمان image_url و imageUrl تا اسپرینگ جفتش را بررسی کند
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
                            System.out.println("🔄 توکن فرانت‌اند پس از ثبت موفق آگهی تمدید شد.");
                        }
                    });

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            UiTheme.toast("آگهی شما با موفقیت ثبت شد و پس از تأیید مدیر نمایش داده می‌شود.");
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
     * ⚙️ ۵. ساخت پکت Multipart/Form-Data برای چند فایل به صورت بایتی
     */
    private byte[] createMultipartBody(List<File> files, String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (File file : files) {
            String fileName = file.getName();
            String mimeType = "image/jpeg";
            try {
                mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) mimeType = "image/jpeg";
            } catch (Exception e) {
                // پیش‌فرض jpeg
            }

            String header = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"files\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: " + mimeType + "\r\n\r\n";

            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(Files.readAllBytes(file.toPath()));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }

        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /**
     * 🔍 ۶. استخراج آدرس تصویر(ها) از پاسخ JSON سرور
     */
    private String extractImageUrlFromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;

        if (!json.contains("{") && json.contains("/")) {
            return json.trim();
        }

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
        if (subCategoryInput != null) {
            subCategoryInput.getSelectionModel().clearSelection();
            subCategoryInput.setVisible(false);
            subCategoryInput.setManaged(false);
        }
        selectedImageFiles.clear();
        mainIdx = 0;
        renderImageThumbs();
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
        UiTheme.styleAlert(alert);
        alert.showAndWait();
    }
}
