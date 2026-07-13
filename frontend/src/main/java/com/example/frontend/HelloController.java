package com.example.frontend;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

    private final ContextMenu hoverMenu = new ContextMenu();
    private final HttpClient client = HttpClient.newHttpClient();

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

        myDivarButton.setOnMouseEntered(event -> {
            if (!hoverMenu.isShowing()) {
                hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 2);
            }
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

        // 🟢 حل مشکل: حالا متد دو ورودیِ کارساز را صدا می‌زنیم تا گفتگو ثبت شود
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
                            // 🟢 اینجا false پاس داده می‌شود چون صفحه اصلی است و همه آگهی‌ها مال ما نیستند
                            renderAdvertisements(response.body(), false);
                        } else {
                            showErrorAlert("خطای سیستم", "امکان دریافت آگهی‌ها از دیتابیس وجود ندارد. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    private String cleanPrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.trim().isEmpty() || priceStr.equals("مشخص نشده")) return "۰";
            java.math.BigDecimal bd = new java.math.BigDecimal(priceStr);
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            return df.format(bd);
        } catch (Exception e) {
            return priceStr;
        }
    }

    // 🟢 اصلاح شد: پارامتر boolean isMyAdsView اضافه شد تا وضعیت صفحه مشخص شود
    private void renderAdvertisements(String responseBody, boolean isMyAdsView) {
        FlowPane gridPane = new FlowPane();
        gridPane.setHgap(18);
        gridPane.setVgap(18);
        gridPane.setPadding(new Insets(25));
        gridPane.setStyle("-fx-background-color: #160f29;");
        gridPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        String targetJson = responseBody;
        int dataIndex = responseBody.indexOf("\"data\"");
        if (dataIndex != -1) {
            targetJson = responseBody.substring(dataIndex);
        }

        Pattern objectPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher objectMatcher = objectPattern.matcher(targetJson);

        int counter = 0;
        while (objectMatcher.find()) {
            String fields = objectMatcher.group(1);

            String idStr = extractJsonField(fields, "id");
            final Long adId = idStr.equals("مشخص نشده") ? null : Long.parseLong(idStr);

            String adOwner = extractJsonField(fields, "ownerUsername").trim();
            String currentUser = MainApplication.currentUsername != null ? MainApplication.currentUsername.trim() : "";

            System.out.println("🔍 [Debug] آگهی شناسه: " + adId + " | مالک آگهی: '" + adOwner + "' | کاربر جاری: '" + currentUser + "'");

            // 🟢 اصلاح شد: اگر در صفحه آگهی‌های من باشیم (isMyAdsView == true)، دکمه‌ها بدون قید و شرط نشان داده می‌شوند
            boolean isOwner = isMyAdsView || FORCE_SHOW_BUTTONS || (!adOwner.equals("مشخص نشده") && adOwner.equalsIgnoreCase(currentUser));

            String title = extractJsonField(fields, "title");
            String description = extractJsonField(fields, "description");
            String price = extractJsonField(fields, "price");

            VBox adCard = new VBox(10);
            adCard.setPrefWidth(230);
            adCard.setPrefHeight(isOwner ? 210 : 160);
            adCard.setStyle(
                    "-fx-background-color: #241942;" +
                            "-fx-border-color: #3b286b;" +
                            "-fx-border-width: 1px;" +
                            "-fx-border-radius: 8px;" +
                            "-fx-background-radius: 8px;" +
                            "-fx-padding: 15px;" +
                            "-fx-cursor: hand;"
            );

            adCard.setOnMouseEntered(e -> adCard.setStyle(adCard.getStyle() + "-fx-border-color: #ffc83b; -fx-background-color: #2c1f52;"));
            adCard.setOnMouseExited(e -> adCard.setStyle(adCard.getStyle().replace("-fx-border-color: #ffc83b; -fx-background-color: #2c1f52;", "-fx-border-color: #3b286b;")));

            adCard.setOnMouseClicked(e -> {
                openAdDetailsPage(adId, title, description, price, adOwner);
            });

            Label lblTitle = new Label(title);
            lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn', 'Segoe UI';");

            Label lblDesc = new Label(description);
            lblDesc.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn', 'Segoe UI';");
            lblDesc.setWrapText(true);
            lblDesc.setMaxHeight(40);

            Label lblPrice = new Label("💰 " + cleanPrice(price) + " تومان");
            lblPrice.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn', 'Segoe UI';");

            adCard.getChildren().addAll(lblTitle, lblDesc, lblPrice);

            if (isOwner) {
                HBox actionsBox = new HBox(8);
                actionsBox.setAlignment(Pos.CENTER_LEFT);
                actionsBox.setPadding(new Insets(5, 0, 0, 0));

                Button btnEdit = new Button("✏️ ویرایش");
                btnEdit.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 4px;");

                Button btnDelete = new Button("🗑️ حذف");
                btnDelete.setStyle("-fx-background-color: #d93838; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 11px; -fx-cursor: hand; -fx-background-radius: 4px;");

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
                        openEditDialog(adId, title, description, price);
                    }
                });

                actionsBox.getChildren().addAll(btnEdit, btnDelete);
                adCard.getChildren().add(actionsBox);
            }

            gridPane.getChildren().add(adCard);
            counter++;
        }

        if (counter == 0) {
            Label noAdLabel = new Label("📭 هنوز هیچ آگهی در دیتابیس ثبت نشده است.");
            noAdLabel.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
            gridPane.getChildren().add(noAdLabel);
        }

        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29; -fx-border-color: transparent;");

        mainBorderPane.setCenter(scrollPane);
        System.out.println("✅ تعداد " + counter + " آگهی با موفقیت تفکیک و رندر شد.");
    }

    private void openAdDetailsPage(Long adId, String title, String description, String rawPrice, String owner) {
        VBox detailsContainer = new VBox(20);
        detailsContainer.setPadding(new Insets(30));
        detailsContainer.setStyle("-fx-background-color: #160f29;");
        detailsContainer.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        Button btnBack = new Button("⬅️ بازگشت به آگهی‌ها");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 16px;");
        btnBack.setOnAction(e -> showHomeScreen());

        VBox infoBox = new VBox(15);
        infoBox.setStyle("-fx-background-color: #241942; -fx-padding: 25px; -fx-background-radius: 10px; -fx-border-color: #3b286b; -fx-border-radius: 10px;");

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

    private void openEditDialog(Long adId, String currentTitle, String currentDesc, String currentPrice) {
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
            if (currentPrice != null && !currentPrice.equals("مشخص نشده")) {
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

        grid.add(new Label("عنوان آگهی:"), 0, 0);
        grid.add(txtTitle, 1, 0);
        grid.add(new Label("توضیحات:"), 0, 1);
        grid.add(txtDesc, 1, 1);
        grid.add(new Label("قیمت (تومان):"), 0, 2);
        grid.add(txtPrice, 1, 2);

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
                onUpdateAdClick(adId, newTitle, newDesc, newPriceStr);
            } catch (Exception ex) {
                showErrorAlert("خطای ورودی", "لطفاً برای قیمت فقط عدد انگلیسی وارد کنید.");
            }
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
    private void onRegisterAdClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register-ad-view.fxml"));
            VBox registerForm = loader.load();

            RegisterAdController registerAdController = loader.getController();
            registerAdController.setHelloController(this);

            mainBorderPane.setCenter(registerForm);
            System.out.println("🔄 فرم ثبت آگهی با موفقیت در لایوت مرکزی رندر شد.");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("خطا در بارگذاری", "مشکلی در باز کردن فرم ثبت آگهی رخ داده است: " + e.getMessage());
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
                            // 🟢 اینجا true پاس داده می‌شود چون صد در صد تمام آگهی‌های برگشتی متعلق به خود کاربر هستند
                            renderAdvertisements(response.body(), true);
                            showSuccessAlert("بارگذاری موفق", "آگهی‌های شما با موفقیت دریافت شد.");
                        } else {
                            showErrorAlert("خطا", "عدم امکان بارگذاری آگهی‌ها. کد: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
    }

    private void onUpdateAdClick(Long adId, String newTitle, String newDesc, String newPrice) {
        String safeTitle = escapeJson(newTitle);
        String safeDesc = escapeJson(newDesc);

        String updatedJson = "{"
                + "\"title\": \"" + safeTitle + "\","
                + "\"description\": \"" + safeDesc + "\","
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
                            showHomeScreen();
                        } else if (response.statusCode() == 403) {
                            showErrorAlert("خطای دسترسی", "شما مالک این آگهی نیستید و اجازه ویرایش آن را ندارید!");
                        } else {
                            showErrorAlert("خطا", "خطا در ویرایش آگهی. کد خطا: " + response.statusCode());
                        }
                    });
                })
                .exceptionally(this::handleNetworkException);
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