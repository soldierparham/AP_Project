package com.example.frontend;

import com.example.frontend.service.HttpService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloController {

    public static final String BASE_URL = "http://localhost:8080";
    private static final boolean FORCE_SHOW_BUTTONS = false;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Button myDivarButton;

    @FXML
    private TextField searchField;

    private final ContextMenu hoverMenu = new ContextMenu();
    private final HttpClient client = HttpClient.newHttpClient();

    // 💾 کش کردن اطلاعات آگهی‌ها برای جستجوی فوق‌سریع و محلی بدون تاخیر سرور
    private String cachedAdsJson = "";
    private boolean cachedIsMyAdsView = false;

    @FXML
    public void initialize() {
        MenuItem profileItem = new MenuItem("👤 پروفایل کاربری");
        MenuItem logoutItem = new MenuItem("🔒 خروج از حساب");

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

        profileItem.setOnAction(event -> handleProfileClick());
        logoutItem.setOnAction(event -> handleLogoutClick());

        // باز شدن منوی شناور در حالت هاور موس روی دکمه دیوار
        myDivarButton.setOnMouseEntered(event -> {
            if (!hoverMenu.isShowing()) {
                hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 2);
            }
        });

        // سیم‌کشی مستقیم دکمه دیوار برای بازگشت به صفحه اصلی در صورت کلیک
        myDivarButton.setOnAction(event -> {
            hoverMenu.hide();
            showHomeScreen();
        });

        showHomeScreen();
    }

    @FXML
    private void onChatScreenClick() {
        System.out.println("🔄 در حال بارگذاری محیط گفتگوها...");
        ChatController chatController = new ChatController();
        HBox chatLayout = chatController.createChatView();
        mainBorderPane.setCenter(chatLayout);
    }

    private void openChatWithUser(Long adId, String targetUsername) {
        System.out.println("🔄 در حال انتقال مستقیم به محیط گفتگو با: " + targetUsername + " برای آگهی با شناسه: " + adId);
        ChatController chatController = new ChatController();
        HBox chatLayout = chatController.createChatView(adId, targetUsername);
        mainBorderPane.setCenter(chatLayout);
    }

    private void handleProfileClick() {
        System.out.println("نمایش مشخصات کاربر: " + MainApplication.currentUsername);
    }

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

    public void showHomeScreen() {
        System.out.println("🔄 در حال دریافت آخرین آگهی‌ها از دیتابیس...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (handleUnauthorized(response.statusCode())) return;

                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            cachedAdsJson = response.body();
                            cachedIsMyAdsView = false;
                            renderAdvertisements(cachedAdsJson, false, "");
                        } else {
                            showErrorAlert("خطای سیستم", "امکان دریافت آگهی‌ها از دیتابیس وجود ندارد. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    private String cleanPrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.trim().isEmpty() || priceStr.equals("مشخص نشده") || priceStr.equals("null")) {
                return "۰";
            }
            java.math.BigDecimal bd = new java.math.BigDecimal(priceStr);
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            return df.format(bd);
        } catch (Exception e) {
            return priceStr;
        }
    }

    // 🔍 رویداد جستجوی آنی همزمان با تایپ کاربر
    @FXML
    private void onSearchKeyReleased() {
        String query = searchField.getText().trim();
        renderAdvertisements(cachedAdsJson, cachedIsMyAdsView, query);
    }

    // 🏷️ فیلتر کردن هوشمند آگهی‌ها بر اساس دسته‌بندی
    @FXML
    private void onCategoryClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        String category = clickedButton.getText();
        System.out.println("🎯 فیلتر دسته بندی روی: " + category);
        searchField.setText(category);
        renderAdvertisements(cachedAdsJson, cachedIsMyAdsView, category);
    }

    // 🟢 متد رندر کارت‌ها با ایمنی ۱۰۰٪ در برابر استثناها و مقادیر تهی
    private void renderAdvertisements(String responseBody, boolean isMyAdsView, String searchQuery) {
        try {
            // گارد ریل برای پاسخ‌های نامعتبر یا خالی
            if (responseBody == null || responseBody.trim().isEmpty()) {
                System.err.println("❌ خطا: اطلاعات آگهی خالی است.");
                return;
            }

            // ایجاد یک ساختار گرید با ۳ ستون شبیه طرح FXML شما
            GridPane gridPane = new GridPane();
            gridPane.setHgap(25);
            gridPane.setVgap(15);
            gridPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

            String targetJson = responseBody;
            int dataIndex = responseBody.indexOf("\"data\"");
            if (dataIndex != -1) {
                targetJson = responseBody.substring(dataIndex);
            }

            Pattern objectPattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher objectMatcher = objectPattern.matcher(targetJson);

            int validAdsCounter = 0;

            while (objectMatcher.find()) {
                String fields = objectMatcher.group(1);

                String title = extractJsonField(fields, "title");
                if (title.equals("null")) title = "بدون عنوان";

                String description = extractJsonField(fields, "description");
                if (description.equals("null")) description = "بدون توضیحات";

                String price = extractJsonField(fields, "price");
                if (price.equals("null")) price = "مشخص نشده";

                String imageUrl = extractJsonField(fields, "imageUrl");

                String adOwner = extractJsonField(fields, "ownerUsername").trim();
                if (adOwner.equals("null")) adOwner = "مشخص نشده";

                String idStr = extractJsonField(fields, "id");
                final Long adId;
                if (idStr.equals("مشخص نشده") || idStr.equals("null")) {
                    adId = null;
                } else {
                    try {
                        adId = Long.parseLong(idStr);
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ خطا در پردازش شناسه عددی آگهی: " + idStr);
                        continue; // پرش ایمن از آگهی معیوب بدون کرش کردن کل برنامه
                    }
                }

                // دریافت مقادیر اضافی برای افزایش دقت فیلترها در فرانت‌اند
                String category = extractJsonField(fields, "category");
                if (category.equals("null")) category = "";

                String city = extractJsonField(fields, "city");
                if (city.equals("null")) city = "";

                if (searchQuery != null && !searchQuery.isEmpty()) {
                    String normalizedQuery = searchQuery.toLowerCase();
                    boolean matches = title.toLowerCase().contains(normalizedQuery)
                            || description.toLowerCase().contains(normalizedQuery)
                            || (!category.isEmpty() && category.toLowerCase().contains(normalizedQuery))
                            || (!city.isEmpty() && city.toLowerCase().contains(normalizedQuery));
                    if (!matches) {
                        continue; // پرش از آگهی در صورت عدم انطباق با فیلتر جستجو
                    }
                }

                String currentUser = MainApplication.currentUsername != null ? MainApplication.currentUsername.trim() : "";
                boolean isOwner = isMyAdsView || FORCE_SHOW_BUTTONS || (!adOwner.equals("مشخص نشده") && adOwner.equalsIgnoreCase(currentUser));

                // 💳 ایجاد کانتینر اصلی کارت به صورت افقی (HBox) با ابعاد بزرگ‌تر و شکیل‌تر
                HBox adCard = new HBox(15);
                adCard.setAlignment(Pos.CENTER_RIGHT);
                adCard.setPrefHeight(170);   // 🚀 تغییر اندازه: ارتفاع از ۱۳۵ به ۱۷۰ بزرگتر شد
                adCard.setMinWidth(380);     // 🚀 تغییر اندازه: عرض حداقلی از ۲۶۰ به ۳۲۰ افزایش یافت
                adCard.setMaxWidth(480);     // 🚀 تغییر اندازه: عرض حداکثری از ۳۴۰ به ۴۲۰ افزایش یافت
                adCard.setStyle(
                        "-fx-background-color: #241942;" +
                                "-fx-background-radius: 8px;" +
                                "-fx-border-color: #3b286b;" +
                                "-fx-border-radius: 8px;" +
                                "-fx-padding: 15px;" +
                                "-fx-cursor: hand;"
                );

                // افکت hover کارت‌ها
                adCard.setOnMouseEntered(e -> adCard.setStyle(adCard.getStyle() + "-fx-border-color: #ffc83b; -fx-background-color: #2c1f52;"));
                adCard.setOnMouseExited(e -> adCard.setStyle(adCard.getStyle().replace("-fx-border-color: #ffc83b; -fx-background-color: #2c1f52;", "-fx-border-color: #3b286b;")));

                final String finalTitle = title;
                final String finalDescription = description;
                final String finalPrice = price;
                final String finalImageUrl = imageUrl;
                final String finalAdOwner = adOwner;

                adCard.setOnMouseClicked(e -> {
                    openAdDetailsPage(adId, finalTitle, finalDescription, finalPrice, finalAdOwner, finalImageUrl);
                });

                // 📝 بخش متنی آگهی در سمت راست (VBox)
                VBox textContainer = new VBox(8); // فاصله بین متن‌ها افزایش یافت
                textContainer.setAlignment(Pos.TOP_RIGHT);
                HBox.setHgrow(textContainer, Priority.ALWAYS);

                Label lblTitle = new Label(title);
                lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn', 'Segoe UI';");
                lblTitle.setWrapText(true);
                lblTitle.setMaxHeight(40);

                Label lblDesc = new Label(description);
                lblDesc.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn', 'Segoe UI';");
                lblDesc.setWrapText(true);
                lblDesc.setMaxHeight(35);

                Label lblPrice = new Label(cleanPrice(price) + " تومان");
                lblPrice.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

                textContainer.getChildren().addAll(lblTitle, lblDesc, new Region() {{ VBox.setVgrow(this, Priority.ALWAYS); }}, lblPrice);

                // نمایش کلید‌های ویرایش و حذف برای مالک آگهی
                // نمایش کلید‌های ویرایش و حذف برای مالک آگهی
                if (isOwner) {
                    HBox actionsBox = new HBox(10); // فاصله بین دکمه‌ها از ۶ به ۱۰ افزایش یافت
                    actionsBox.setAlignment(Pos.CENTER_RIGHT);

                    // دکمه ویرایش بزرگ‌تر و زیباتر با افکت هاور
                    Button btnEdit = new Button("✏️ ویرایش");
                    btnEdit.setStyle(
                            "-fx-background-color: #3b286b; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 14px; " + // سایز بزرگ‌تر
                                    "-fx-cursor: hand; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-padding: 6px 14px;" // پدینگ بزرگ‌تر
                    );
                    btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnEdit.getStyle() + "-fx-background-color: #4c348a;"));
                    btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnEdit.getStyle().replace("-fx-background-color: #4c348a;", "")));

                    // دکمه حذف بزرگ‌تر و زیباتر با افکت هاور
                    Button btnDelete = new Button("🗑️ حذف");
                    btnDelete.setStyle(
                            "-fx-background-color: #d93838; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 14px; " + // سایز بزرگ‌تر
                                    "-fx-cursor: hand; " +
                                    "-fx-background-radius: 6px; " +
                                    "-fx-padding: 6px 14px;" // پدینگ بزرگ‌تر
                    );
                    btnDelete.setOnMouseEntered(e -> btnDelete.setStyle(btnDelete.getStyle() + "-fx-background-color: #f24e4e;"));
                    btnDelete.setOnMouseExited(e -> btnDelete.setStyle(btnDelete.getStyle().replace("-fx-background-color: #f24e4e;", "")));

                    btnEdit.setOnMouseClicked(Event::consume);
                    btnDelete.setOnMouseClicked(Event::consume);

                    btnDelete.setOnAction(e -> {
                        if (adId != null) {
                            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "آیا از حذف این آگهی اطمینان دارید؟", ButtonType.YES, ButtonType.NO);
                            confirmAlert.setHeaderText(null);
                            confirmAlert.showAndWait().ifPresent(response -> {
                                if (response == ButtonType.YES) {
                                    onDeleteAdClick(adId);
                                }
                            });
                        }
                    });

                    btnEdit.setOnAction(e -> {
                        if (adId != null) {
                            openEditDialog(adId, finalTitle, finalDescription, finalPrice, finalImageUrl);
                        }
                    });

                    actionsBox.getChildren().addAll(btnEdit, btnDelete);
                    textContainer.getChildren().add(actionsBox);
                }

                // 🖼️ بخش تصویر در سمت چپ (StackPane) با ابعاد بزرگ‌تر
                StackPane imageContainer = new StackPane();
                imageContainer.setPrefSize(140, 140);
                imageContainer.setMinSize(140, 140);
                imageContainer.setMaxSize(140, 140);
                imageContainer.setStyle(
                        "-fx-background-color: #160f29;" +
                                "-fx-border-color: #3b286b;" +
                                "-fx-border-width: 1px;" +
                                "-fx-background-radius: 6px;" +
                                "-fx-border-radius: 6px;"
                );

                ImageView adImageView = new ImageView();
                adImageView.setFitWidth(130);
                adImageView.setFitHeight(130);
                adImageView.setPreserveRatio(true);

                if (imageUrl != null && !imageUrl.equals("مشخص نشده") && !imageUrl.equals("null") && !imageUrl.trim().isEmpty()) {
                    // جدا کردن آدرس‌ها با کاما و انتخاب اولین تصویر برای نمایش روی کارت آگهی
                    String[] allImages = imageUrl.split(",");
                    String cleanUrl = allImages[0].trim(); // 💡 تعریف درست متغیر cleanUrl از روی اولین عکس

                    if (cleanUrl.startsWith("/")) {
                        cleanUrl = cleanUrl.substring(1);
                    }
                    String finalPath = BASE_URL + "/" + cleanUrl;

                    System.out.println("🖼️ [DEBUG] رندر تصویر کارت از: " + finalPath);
                    Image image = new Image(finalPath, true);
                    adImageView.setImage(image);
                } else {
                    try {
                        adImageView.setImage(new Image(getClass().getResourceAsStream("/images/no-image.png")));
                    } catch (Exception ex) {
                        // نادیده گرفتن خطا
                    }
                }

// ✅ فقط یک‌بار فرزند را به والد اضافه می‌کنیم
                imageContainer.getChildren().add(adImageView);

// قابلیت کلیک روی عکس جهت باز شدن پیش‌نمایش بزرگ
                final String finalImageUrlForPreview = imageUrl;
                imageContainer.setOnMouseClicked(e -> {
                    e.consume();
                    showImagePreview(finalImageUrlForPreview);
                });

                // ادغام اطلاعات کارت
                adCard.getChildren().addAll(textContainer, imageContainer);

                int column = validAdsCounter % 2;
                int row = validAdsCounter / 2;
                gridPane.add(adCard, column, row);
                validAdsCounter++;
            }

            // پکیج‌بندی نهایی داخل ScrollPane
            VBox layoutContainer = new VBox(15);
            layoutContainer.setStyle("-fx-background-color: #160f29;");
            layoutContainer.setPadding(new Insets(25, 25, 25, 10));

            String headerTitle = isMyAdsView ? "آگهی‌های ثبت شده شما" : "آگهی‌های تازه دپارتمان";
            Label lblHeader = new Label(headerTitle);
            lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

            layoutContainer.getChildren().addAll(lblHeader, gridPane);

            if (validAdsCounter == 0) {
                Label noAdLabel = new Label("📭 هیچ آگهی متناسب با درخواست شما یافت نشد.");
                noAdLabel.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
                layoutContainer.getChildren().add(noAdLabel);
            }

            ScrollPane scrollPane = new ScrollPane(layoutContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29; -fx-border-color: transparent;");

            mainBorderPane.setCenter(scrollPane);
            System.out.println("✅ تعداد " + validAdsCounter + " آگهی در طرح افقی بزرگ رندر شد.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ خطا در اجرای رندر آگهی‌ها: " + e.getMessage());
            showErrorAlert("خطا در سیستم رندر", "هنگام پردازش ساختار آگهی‌ها مشکلی پیش آمد.");
        }
    }

    // 🖼️ متد جدید باز کردن پیش‌نمایش تصویر به صورت پاپ‌آپ مدرن و هماهنگ با دیزاین بنفش
// 🖼️ ۲. اصلاح متد باز کردن پیش‌نمایش تصویر به صورت پاپ‌آپ مدرن
    private void showImagePreview(String imageUrl) {
        Dialog<Void> previewDialog = new Dialog<>();
        previewDialog.setTitle("پیش‌نمایش تصویر کالا");

        ButtonType closeButton = new ButtonType("بستن صفحه", ButtonBar.ButtonData.CANCEL_CLOSE);
        previewDialog.getDialogPane().getButtonTypes().add(closeButton);

        ImageView largeImageView = new ImageView();
        largeImageView.setFitWidth(550);
        largeImageView.setFitHeight(450);
        largeImageView.setPreserveRatio(true);

        if (imageUrl != null && !imageUrl.equals("مشخص نشده") && !imageUrl.equals("null") && !imageUrl.trim().isEmpty()) {
            String cleanUrl = imageUrl.trim();
            if (cleanUrl.startsWith("/")) {
                cleanUrl = cleanUrl.substring(1);
            }
            String finalPath = BASE_URL + "/" + cleanUrl;
            System.out.println("🖼️ [DEBUG] پیش‌نمایش بزرگ تصویر از: " + finalPath);
            largeImageView.setImage(new Image(finalPath, true));
        } else {
            try {
                largeImageView.setImage(new Image(getClass().getResourceAsStream("/images/no-image.png")));
            } catch (Exception ex) {
                return;
            }
        }

        VBox container = new VBox(largeImageView);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: #160f29;");

        DialogPane dialogPane = previewDialog.getDialogPane();
        dialogPane.setContent(container);
        dialogPane.setStyle("-fx-background-color: #160f29; -fx-border-color: #3b286b; -fx-border-width: 1px;");

        dialogPane.lookupButton(closeButton).setStyle(
                "-fx-background-color: #3b286b;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-family: 'Vazirmatn';" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;"
        );

        previewDialog.showAndWait();
    }
    private void openAdDetailsPage(Long adId, String title, String description, String rawPrice, String owner, String imageUrl) {
        VBox detailsContainer = new VBox(20);
        detailsContainer.setPadding(new Insets(30));
        detailsContainer.setStyle("-fx-background-color: #160f29;");
        detailsContainer.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Button btnBack = new Button("⬅️ بازگشت به آگهی‌ها");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 16px;");
        btnBack.setOnAction(e -> showHomeScreen());

        VBox infoBox = new VBox(15);
        infoBox.setStyle("-fx-background-color: #241942; -fx-padding: 25px; -fx-background-radius: 10px; -fx-border-color: #3b286b; -fx-border-radius: 10px;");

        if (imageUrl != null && !imageUrl.equals("مشخص نشده") && !imageUrl.equals("null") && !imageUrl.trim().isEmpty()) {
            ImageView detailsImageView = new ImageView();
            detailsImageView.setFitWidth(400);
            detailsImageView.setFitHeight(250);
            detailsImageView.setPreserveRatio(true);
            detailsImageView.setStyle("-fx-border-color: #3b286b; -fx-border-width: 1px; -fx-border-radius: 8px;");

            String cleanUrl = imageUrl.trim();
            if (cleanUrl.startsWith("/")) {
                cleanUrl = cleanUrl.substring(1);
            }
            String finalPath = BASE_URL + "/" + cleanUrl;
            System.out.println("🖼️ [DEBUG] لود تصویر جزئیات از: " + finalPath);

            Image img = new Image(finalPath, true);
            detailsImageView.setImage(img);

            VBox imgWrapper = new VBox(detailsImageView);
            imgWrapper.setAlignment(Pos.CENTER);
            imgWrapper.setPadding(new Insets(0, 0, 15, 0));
            infoBox.getChildren().add(imgWrapper);
        }

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");
        lblTitle.setWrapText(true);

        Label lblPrice = new Label("💰 قیمت آگهی: " + cleanPrice(rawPrice) + " تومان");
        lblPrice.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblOwner = new Label("👤 نام کاربری آگهی‌دهنده: " + owner);
        lblOwner.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");

        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #3b286b;");

        Label lblDescTitle = new Label("📋 توضیحات آگهی:");
        lblDescTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblDesc = new Label(description);
        lblDesc.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn'; -fx-line-spacing: 6px;");
        lblDesc.setWrapText(true);

        Button btnStartChat = new Button("💬 شروع گفتگو با آگهی‌دهنده");
        btnStartChat.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 10px 20px;");

        btnStartChat.setOnAction(e -> openChatWithUser(adId, owner));

        infoBox.getChildren().addAll(lblTitle, lblPrice, lblOwner, separator, lblDescTitle, lblDesc, btnStartChat);
        detailsContainer.getChildren().addAll(btnBack, infoBox);

        ScrollPane scrollPane = new ScrollPane(detailsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29; -fx-border-color: transparent;");

        mainBorderPane.setCenter(scrollPane);
    }

    private void openEditDialog(Long adId, String currentTitle, String currentDesc, String currentPrice, String currentImageUrl) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("✏️ ویرایش آگهی");
        dialog.setHeaderText("مشخصات جدید آگهی را وارد کنید:");

        ButtonType saveButtonType = new ButtonType("💾 ذخیره تغییرات", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));
        grid.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        String plainPrice = currentPrice;
        try {
            if (currentPrice != null && !currentPrice.equals("مشخص نشده") && !currentPrice.equals("null")) {
                plainPrice = new java.math.BigDecimal(currentPrice).toPlainString();
            }
        } catch (Exception e) {
            plainPrice = currentPrice;
        }

        TextField txtTitle = new TextField(currentTitle);
        TextArea txtDesc = new TextArea(currentDesc);
        txtDesc.setPrefRowCount(3);
        txtDesc.setWrapText(true);
        TextField txtPrice = new TextField(plainPrice);

        // 📁 ۱. استفاده از لیست به جای آرایه تک المانی برای ذخیره چندین فایل
        final java.util.List<File> selectedNewImageFiles = new java.util.ArrayList<>();

        // 🖼️ ۲. ایجاد یک کانتینر افقی با فاصله مناسب برای نمایش تمام پیش‌نمایش‌ها
        HBox imagesPreviewContainer = new HBox(8);
        imagesPreviewContainer.setAlignment(Pos.CENTER_LEFT);

        Label lblImgPath = new Label("تصاویر قبلی تغییر نخواهند کرد.");
        lblImgPath.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 11px;");

        // لود تصاویر قبلی آگهی (در صورتی که چند عکس با کاما جدا شده باشند)
        if (currentImageUrl != null && !currentImageUrl.equals("مشخص نشده") && !currentImageUrl.equals("null") && !currentImageUrl.trim().isEmpty()) {
            String[] oldUrls = currentImageUrl.split(",");
            for (String oldUrl : oldUrls) {
                String cleanUrl = oldUrl.trim();

                // اصلاح خطای محدوده متغیر با اعمال شرط روی همان متغیر
                if (cleanUrl.startsWith("/")) {
                    cleanUrl = cleanUrl.substring(1);
                }

                // اکنون cleanUrl بدون مشکل در اینجا قابل دسترسی است
                ImageView oldImgView = new ImageView(new Image(BASE_URL + "/" + cleanUrl, true));
                oldImgView.setFitWidth(60);
                oldImgView.setFitHeight(45);
                oldImgView.setPreserveRatio(true);
                imagesPreviewContainer.getChildren().add(oldImgView);
            }
        }

        Button btnSelectImg = new Button("🔄 انتخاب عکس‌های جدید");
        btnSelectImg.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 11px; -fx-cursor: hand;");
        btnSelectImg.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("انتخاب عکس‌های جدید آگهی");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("تصاویر", "*.png", "*.jpg", "*.jpeg")
            );

            // 🚀 ۳. فعال‌سازی قابلیت انتخاب چند عکس به صورت هم‌زمان
            java.util.List<File> files = fileChooser.showOpenMultipleDialog(dialog.getOwner());
            if (files != null && !files.isEmpty()) {
                selectedNewImageFiles.clear();
                imagesPreviewContainer.getChildren().clear();
                selectedNewImageFiles.addAll(files);

                lblImgPath.setText(files.size() + " عکس جدید انتخاب شد.");

                // رندر داینامیک پیش‌نمایش عکس‌های انتخاب شده
                for (File file : files) {
                    ImageView imgView = new ImageView(new Image(file.toURI().toString()));
                    imgView.setFitWidth(60);
                    imgView.setFitHeight(45);
                    imgView.setPreserveRatio(true);
                    imagesPreviewContainer.getChildren().add(imgView);
                }
            }
        });

        HBox imageActionBox = new HBox(10);
        imageActionBox.setAlignment(Pos.CENTER_LEFT);
        imageActionBox.getChildren().addAll(btnSelectImg, imagesPreviewContainer, lblImgPath);

        grid.add(new Label("عنوان آگهی:"), 0, 0);
        grid.add(txtTitle, 1, 0);
        grid.add(new Label("توضیحات:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("قیمت (تومان):"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("تصویر آگهی:"), 0, 3);
        grid.add(imageActionBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                String newTitle = txtTitle.getText().trim();
                String newDesc = txtDesc.getText().trim();
                String newPriceStr = txtPrice.getText().trim();

                if (newTitle.isEmpty() || newDesc.isEmpty() || newPriceStr.isEmpty()) {
                    showErrorAlert("خطا", "فیلدها نمی‌توانند خالی بمانند.");
                    return;
                }

                new java.math.BigDecimal(newPriceStr);

                // 🚀 ۴. ارسال لیست تصاویر به متد مالتی‌پارت در صورت انتخاب عکس‌های جدید
                if (!selectedNewImageFiles.isEmpty()) {
                    System.out.println("📤 در حال آپلود تصاویر جدید ویرایش شده...");
                    HttpService.uploadMultipleFiles("/api/upload", selectedNewImageFiles)
                            .thenAccept(uploadResponse -> {
                                if (uploadResponse.statusCode() == 200 || uploadResponse.statusCode() == 201) {
                                    String newUploadedUrls = extractImageUrlFromJson(uploadResponse.body());
                                    Platform.runLater(() -> {
                                        onUpdateAdClick(adId, newTitle, newDesc, newPriceStr, newUploadedUrls);
                                    });
                                } else {
                                    Platform.runLater(() -> showErrorAlert("خطای آپلود", "آپلود تصاویر جدید با خطا مواجه شد."));
                                }
                            })
                            .exceptionally(ex -> {
                                Platform.runLater(() -> showErrorAlert("خطا", "ارتباط ناموفق با سرور برای آپلود: " + ex.getMessage()));
                                return null;
                            });
                } else {
                    // سناریوی بدون تغییر عکس: همان لیست آدرس‌های رشته‌ای قبلی ارسال می‌شود
                    onUpdateAdClick(adId, newTitle, newDesc, newPriceStr, currentImageUrl);
                }

            } catch (Exception ex) {
                showErrorAlert("خطای ورودی", "لطفاً برای قیمت فقط عدد انگلیسی وارد کنید.");
            }
        }
    }
    private String extractImageUrlFromJson(String json) {
        try {
            int index = json.indexOf("\"imageUrl\"");
            if (index == -1) return null;
            int colonIndex = json.indexOf(":", index);
            if (colonIndex == -1) return null;
            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) return null;
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (endQuote == -1) return null;
            return json.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String extractJsonField(String source, String key) {
        Pattern pString = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
        Matcher mString = pString.matcher(source);
        if (mString.find()) {
            return mString.group(1).trim()
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\");
        }
        Pattern pNumeric = Pattern.compile("\"" + key + "\"\\s*:\\s*([^,\\s}]+)");
        Matcher mNumeric = pNumeric.matcher(source);
        if (mNumeric.find()) {
            return mNumeric.group(1).trim();
        }
        return "مشخص نشده";
    }

    @FXML
    private void onRegisterAdScreenClick() {
        try {
            System.out.println("🔄 در حال بارگذاری فرم ثبت آگهی جدید...");

            // ✅ اصلاح نام فایل به register-ad-view.fxml به صورت آدرس‌دهی نسبی
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register-ad-view.fxml"));

            VBox registerLayout = loader.load();

            RegisterAdController controller = loader.getController();
            controller.setHelloController(this);

            mainBorderPane.setCenter(registerLayout);

            System.out.println("✅ فرم ثبت آگهی با موفقیت تزریق و فعال شد.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ خطا در بارگذاری صفحه ثبت آگهی: " + e.getMessage());
            showErrorAlert("خطای بارگذاری", "امکان باز کردن فرم ثبت آگهی وجود ندارد.");
        }
    }

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
                            cachedAdsJson = response.body();
                            cachedIsMyAdsView = true;
                            renderAdvertisements(cachedAdsJson, true, "");
                            showSuccessAlert("بارگذاری موفق", "آگهی‌های شما با موفقیت دریافت شد.");
                        } else {
                            showErrorAlert("خطا", "عدم امکان بارگذاری آگهی‌ها. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    private void onUpdateAdClick(Long adId, String title, String description, String price, String imageUrl) {
        try {
            System.out.println("🔄 در حال ارسال درخواست ویرایش آگهی به سرور...");

            // ساخت JSON بادی به صورت دستی و ایمن
            String jsonBody = String.format(
                    "{\"id\":%d,\"title\":\"%s\",\"description\":\"%s\",\"price\":%s,\"imageUrl\":\"%s\"}",
                    adId, title, description, price, imageUrl
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + MainApplication.jwtToken) // 🔑 ارسال توکن تازه
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200 || response.statusCode() == 204) {
                                showHomeScreen(); // 🔄 رفرش آنی صفحه اصلی برای نمایش اطلاعات جدید
                                System.out.println("✅ آگهی با موفقیت ویرایش و به روزرسانی شد.");
                            } else {
                                showErrorAlert("خطای ویرایش", "سرور درخواست ویرایش را نپذیرفت. کد: " + response.statusCode());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showErrorAlert("خطای شبکه", "ارتباط با سرور برقرار نشد: " + ex.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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
                            showHomeScreen();
                        } else if (response.statusCode() == 403) {
                            showErrorAlert("خطای دسترسی", "شما اجازه حذف این آگهی را ندارید!");
                        } else {
                            showErrorAlert("خطا", "حذف آگهی با خطا مواجه شد. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    private void checkAndRefreshToken(HttpResponse<?> response) {
        response.headers().firstValue("Authorization").ifPresent(authHeader -> {
            if (authHeader.startsWith("Bearer ")) {
                String newToken = authHeader.substring(7);
                MainApplication.jwtToken = newToken;
                System.out.println("🔄 [JavaFX] توکن تازه‌نفس دریافت و تمدید شد.");
            }
        });
    }

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

    private Void handleNetworkException(Throwable e) {
        Platform.runLater(() -> showErrorAlert("خطا در اتصال", "خطا در ارتباط با سرور: " + e.getMessage()));
        return null;
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}