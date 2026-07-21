package com.example.frontend;

import com.example.frontend.service.HttpService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🏠 کنترلر صفحه اصلی — نسخه تکمیل‌شده مطابق سند پروژه:
 * ⭐ علاقه‌مندی‌ها، ⭐ امتیازدهی به فروشنده، ✅ فروخته شد،
 * 🔍 فیلترهای ترکیبی و مرتب‌سازی، 🛡️ دسترسی به پنل مدیریت برای ADMIN،
 * 🏷️ نمایش وضعیت آگهی‌ها در بخش «آگهی‌های من» و 💬 چت داخلی.
 */
public class HelloController {

    // ⚠️ باید public بماند؛ کلاس‌های دیگر (مثل RegisterAdController) از HelloController.BASE_URL استفاده می‌کنند
    public static final String BASE_URL = "http://localhost:8080";

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private Button myDivarButton;
    @FXML
    private Button btnRegisterNav;
    @FXML
    private Button btnChatNav;
    @FXML
    private VBox categoryVBox;

    @FXML
    private TextField searchField;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ContextMenu hoverMenu = new ContextMenu();

    private String cachedAdsJson = "";
    private boolean cachedIsMyAdsView = false;
    private boolean cachedIsFavoritesView = false;

    // 🔍 کنترل‌های فیلتر و مرتب‌سازی در ردیف بالا
    @FXML private ComboBox<String> cityFilterCombo;
    @FXML private ComboBox<String> sortFilterCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;

    // 🔍 فیلترهای ترکیبی (امتیازی)
    private String filterCategory = "";
    private String filterCity = "";
    private String filterMinPrice = "";
    private String filterMaxPrice = "";
    private String filterSort = "newest";

    // ---------------------------------------------------------- شروع

    // 💾 کش کردن اطلاعات آگهی‌ها برای جستجوی فوق‌سریع و محلی بدون تاخیر سرور
    private String cachedAdsJson = "";
    private boolean cachedIsMyAdsView = false;

    @FXML
    public void initialize() {
        // 🔑 اصلاح مهم: نام کاربری واقعی از توکن JWT خوانده می‌شود.
        // پاسخ لاگین فیلد username ندارد و LoginController شماره تلفن را ذخیره می‌کرد؛
        // در نتیجه مقایسه فرستنده پیام با کاربر جاری همیشه شکست می‌خورد و همه پیام‌ها سمت چپ می‌افتادند.
        String tokenUsername = extractUsernameFromToken();
        if (!tokenUsername.isBlank()) {
            MainApplication.currentUsername = tokenUsername;
        }

        MenuItem profileItem = new MenuItem("\ud83d\udc64 آگهی‌های من");
        profileItem.setOnAction(e -> onLoadMyAdsClick());

        MenuItem favoritesItem = new MenuItem("\u2b50 علاقه‌مندی‌های من");
        favoritesItem.setOnAction(e -> showFavoritesScreen());

        MenuItem logoutItem = new MenuItem("\ud83d\udeaa خروج از حساب");
        logoutItem.setOnAction(e -> onLogoutClick());

        hoverMenu.getItems().addAll(profileItem, favoritesItem);

        // 🛡️ فقط برای مدیر سیستم
        if ("ADMIN".equalsIgnoreCase(extractRoleFromToken())) {
            MenuItem adminItem = new MenuItem("\ud83d\udee1\ufe0f پنل مدیریت");
            adminItem.setOnAction(e -> openAdminPanel());
            hoverMenu.getItems().add(adminItem);
        }

        hoverMenu.getItems().add(logoutItem);

        if (myDivarButton != null) {
            myDivarButton.setOnMouseClicked(e ->
                    hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 5));
        }

        if (searchField != null) {
            searchField.setOnAction(e -> refreshCurrentListView());
        }

        // 🔍 مقداردهی کنترل‌های فیلتر و مرتب‌سازی ردیف بالا
        if (cityFilterCombo != null) {
            cityFilterCombo.getItems().addAll("همه شهرها", "تهران", "مشهد", "اصفهان", "شیراز", "تبریز", "کرج", "اهواز", "قم", "کرمانشاه", "ارومیه", "رشت");
        }
        if (sortFilterCombo != null) {
            sortFilterCombo.getItems().addAll("جدیدترین", "ارزان‌ترین", "گران‌ترین");
        }

        setActiveTopBarSection("home");
        updateCategoryButtonStyles(null);
        showHomeScreen();
    }

    // ---------------------------------------------------------- صفحه اصلی

    /** 🔶 تنظیم کادر طلایی دور دکمه فعال در نوار بالا */
    private void setActiveTopBarSection(String section) {
        String normal = "-fx-background-color: transparent; -fx-text-fill: #b9a6df; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-radius: 6;";
        String active = "-fx-background-color: transparent; -fx-text-fill: #ffc83b; -fx-cursor: hand; -fx-background-radius: 6; -fx-border-color: #ffc83b; -fx-border-radius: 6; -fx-border-width: 1.5; -fx-padding: 6 14 6 14;";
        String normalMD = "-fx-background-color: transparent; -fx-text-fill: #b9a6df; -fx-cursor: hand;";
        String activeMD = "-fx-background-color: transparent; -fx-text-fill: #ffc83b; -fx-cursor: hand; -fx-border-color: #ffc83b; -fx-border-radius: 6; -fx-border-width: 1.5; -fx-padding: 4 8 4 8;";
        if (myDivarButton != null) myDivarButton.setStyle(normalMD);
        if (btnRegisterNav != null) btnRegisterNav.setStyle(normal);
        if (btnChatNav != null) btnChatNav.setStyle(normal);
        switch (section) {
            case "myDivar": if (myDivarButton != null) myDivarButton.setStyle(activeMD); break;
            case "register": if (btnRegisterNav != null) btnRegisterNav.setStyle(active); break;
            case "chat": if (btnChatNav != null) btnChatNav.setStyle(active); break;
            default: break;
        }
    }

    /** 🔶 به‌روزکردن کادر دسته‌بندی فعال در ستون سمت راست */
    private void updateCategoryButtonStyles(Button activeBtn) {
        if (categoryVBox == null) return;
        String normal = "-fx-background-color: transparent; -fx-text-fill: #b9a6df; -fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
        String active = "-fx-background-color: transparent; -fx-text-fill: #ffc83b; -fx-cursor: hand; -fx-border-color: #ffc83b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5;";
        for (javafx.scene.Node node : categoryVBox.getChildren()) {
            if (node instanceof Button) node.setStyle(normal);
        }
        if (activeBtn != null) activeBtn.setStyle(active);
    }

    public void showHomeScreen() {
        setActiveTopBarSection("home");
        String query = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim() : "";
        fetchHomeAds(query);
    }

    @FXML
    protected void onSearchClick() {
        refreshCurrentListView();
    }

    /**
     * 🔍 جستجوی زنده هنگام تایپ در فیلد جستجو (متصل به onKeyReleased در FXML)
     * در هر صفحه‌ای که باشید (اصلی یا آگهی‌های من) همان‌جا جستجو می‌کند
     */
    @FXML
    protected void onSearchKeyReleased() {
        refreshCurrentListView();
    }

    /**
     * 🔍 تغییر فیلترها/مرتب‌سازی در ردیف بالا — روی همان صفحه فعلی اعمال می‌شود
     */
    @FXML
    protected void onTopBarFilterChanged() {
        String cityVal = (cityFilterCombo != null && cityFilterCombo.getValue() != null) ? cityFilterCombo.getValue() : "";
        filterCity = "همه شهرها".equals(cityVal) ? "" : cityVal;

        String minP = (minPriceField != null && minPriceField.getText() != null) ? minPriceField.getText().trim() : "";
        String maxP = (maxPriceField != null && maxPriceField.getText() != null) ? maxPriceField.getText().trim() : "";
        // فقط مقادیر عددی معتبر به سرور ارسال می‌شود
        filterMinPrice = minP.matches("\\d+(\\.\\d+)?") ? minP : "";
        filterMaxPrice = maxP.matches("\\d+(\\.\\d+)?") ? maxP : "";

        String sortLabel = (sortFilterCombo != null) ? sortFilterCombo.getValue() : null;
        if ("ارزان‌ترین".equals(sortLabel)) {
            filterSort = "cheapest";
        } else if ("گران‌ترین".equals(sortLabel)) {
            filterSort = "expensive";
        } else {
            filterSort = "newest";
        }

        refreshCurrentListView();
    }

    /**
     * 🔄 تازه‌سازی همان صفحه‌ای که کاربر در آن است — اگر در «آگهی‌های من» باشد، همان‌جا فیلتر می‌شود و به صفحه اول برنمی‌گردد
     */
    private void refreshCurrentListView() {
        if (cachedIsMyAdsView) {
            onLoadMyAdsClick();
        } else if (cachedIsFavoritesView) {
            showFavoritesScreen();
        } else {
            showHomeScreen();
        }
    }

    /**
     * 🗂️ کلیک روی دکمه‌های دسته‌بندی در ستون راست (متصل به onAction در FXML)
     * کلیک مجدد روی همان دسته = حذف فیلتر
     */
    @FXML
    protected void onCategoryClick(ActionEvent event) {
        if (event.getSource() instanceof Button) {
            Button clicked = (Button) event.getSource();
            String category = clicked.getText();
            filterCategory = "همه محصولات".equals(category) ? "" : category;
            updateCategoryButtonStyles("همه محصولات".equals(category) ? null : clicked);
            refreshCurrentListView();
        }
    }

    /**
     * 💬 دکمه «چت» در نوار بالا — نمایش لیست گفتگوهای کاربر
     */
    @FXML
    protected void onChatScreenClick() {
        setActiveTopBarSection("chat");
        client.sendAsync(authorizedGet(BASE_URL + "/api/chat/conversations"), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        renderConversationsList(response.body());
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private void renderConversationsList(String responseBody) {
        VBox listBox = new VBox(10);
        listBox.setPadding(new Insets(20));
        listBox.setStyle("-fx-background-color: #160f29;");

        Label lblHeader = new Label("\ud83d\udcac گفتگوهای شما");
        lblHeader.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Button btnHomeList = new Button("\ud83c\udfe0 صفحه اصلی");
        btnHomeList.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnHomeList.setOnAction(e -> showHomeScreen());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, lblHeader, headerSpacer, btnHomeList);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        listBox.getChildren().add(headerRow);

        int dataIndex = responseBody.indexOf("\"data\"");
        String dataPart = dataIndex >= 0 ? responseBody.substring(dataIndex) : responseBody;

        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(dataPart);
        boolean anyFound = false;
        while (matcher.find()) {
            String convJson = matcher.group(1);
            if (!convJson.contains("\"buyerUsername\"")) continue;
            anyFound = true;

            String convIdStr = extractJsonField(convJson, "id");
            String buyer = extractJsonField(convJson, "buyerUsername");
            String seller = extractJsonField(convJson, "sellerUsername");
            String adTitle = extractJsonField(convJson, "adTitle");
            String otherUser = buyer.equals(MainApplication.currentUsername) ? seller : buyer;

            Button btnConv = new Button("\ud83d\udcac گفتگو با " + otherUser + " — " + adTitle);
            btnConv.setMaxWidth(Double.MAX_VALUE);
            btnConv.setStyle("-fx-background-color: #241942; -fx-text-fill: white; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-radius: 10; -fx-cursor: hand; -fx-alignment: center-right; -fx-padding: 12; -fx-font-family: 'Vazirmatn';");
            btnConv.setOnAction(e -> {
                try {
                    openChatPane(Long.parseLong(convIdStr), otherUser, adTitle);
                } catch (NumberFormatException ignored) {
                }
            });
            listBox.getChildren().add(btnConv);
        }

        if (!anyFound) {
            Label lblEmpty = new Label("هنوز گفتگویی ندارید. از صفحه جزئیات آگهی می‌توانید گفتگو را شروع کنید.");
            lblEmpty.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
            listBox.getChildren().add(lblEmpty);
        }

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
    }

    private void fetchHomeAds(String searchQuery) {
        StringBuilder url = new StringBuilder(BASE_URL + "/api/advertisements?sort=" + filterSort);
        if (!searchQuery.isBlank()) url.append("&search=").append(encode(searchQuery));
        if (!filterCategory.isBlank()) url.append("&category=").append(encode(filterCategory));
        if (!filterCity.isBlank()) url.append("&city=").append(encode(filterCity));
        if (!filterMinPrice.isBlank()) url.append("&minPrice=").append(encode(filterMinPrice));
        if (!filterMaxPrice.isBlank()) url.append("&maxPrice=").append(encode(filterMaxPrice));

        client.sendAsync(authorizedGet(url.toString()), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        cachedAdsJson = response.body();
                        cachedIsMyAdsView = false;
                        cachedIsFavoritesView = false;
                        renderAdvertisements(response.body(), false, searchQuery);
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert("خطا در دریافت آگهی‌ها (کد " + response.statusCode() + ")");
                    }
                }));
    }

    // ---------------------------------------------------------- آگهی‌های من

    @FXML
    protected void onLoadMyAdsClick() {
        setActiveTopBarSection("myDivar");
        // 🔍 فیلترها و جستجوی ردیف بالا روی آگهی‌های من هم اعمال می‌شود
        StringBuilder myUrl = new StringBuilder(BASE_URL + "/api/advertisements/my?sort=" + filterSort);
        String query = (searchField != null && searchField.getText() != null) ? searchField.getText().trim() : "";
        if (!query.isBlank()) myUrl.append("&search=").append(encode(query));
        if (!filterCategory.isBlank()) myUrl.append("&category=").append(encode(filterCategory));
        if (!filterCity.isBlank()) myUrl.append("&city=").append(encode(filterCity));
        if (!filterMinPrice.isBlank()) myUrl.append("&minPrice=").append(encode(filterMinPrice));
        if (!filterMaxPrice.isBlank()) myUrl.append("&maxPrice=").append(encode(filterMaxPrice));

        client.sendAsync(authorizedGet(myUrl.toString()), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        cachedAdsJson = response.body();
                        cachedIsMyAdsView = true;
                        cachedIsFavoritesView = false;
                        renderAdvertisements(response.body(), true, "");
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert("خطا در دریافت آگهی‌های شما (کد " + response.statusCode() + ")");
                    }
                }));
    }

    // ---------------------------------------------------------- ⭐ علاقه‌مندی‌ها

    private void showFavoritesScreen() {
        setActiveTopBarSection("myDivar");
        client.sendAsync(authorizedGet(BASE_URL + "/api/favorites"), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        cachedAdsJson = response.body();
                        cachedIsMyAdsView = false;
                        cachedIsFavoritesView = true;
                        renderAdvertisements(response.body(), false, "");
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private void toggleFavorite(long adId) {
        boolean removing = cachedIsFavoritesView;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/favorites/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken);

        HttpRequest request = removing
                ? builder.DELETE().build()
                : builder.POST(HttpRequest.BodyPublishers.noBody()).build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        if (removing) {
                            showFavoritesScreen();
                        } else {
                            showSuccessAlert("آگهی به علاقه‌مندی‌های شما اضافه شد.");
                        }
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    // ---------------------------------------------------------- ✅ فروخته شد

    private void markAdAsSold(long adId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId + "/sold"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        showSuccessAlert("آگهی با موفقیت به وضعیت «فروخته شده» تغییر کرد.");
                        onLoadMyAdsClick();
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    // ---------------------------------------------------------- ابزارهای عمومی

    private HttpRequest authorizedGet(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void checkAndRefreshToken(HttpResponse<String> response) {
        Optional<String> header = response.headers().firstValue("Authorization");
        header.ifPresent(h -> {
            String token = h.startsWith("Bearer ") ? h.substring(7) : h;
            if (!token.isBlank()) {
                MainApplication.jwtToken = token;
            }
        });
    }

    private void handleUnauthorized(int statusCode) {
        Stage stage = (Stage) mainBorderPane.getScene().getWindow();
        MainApplication.redirectToLogin(stage, "نشست شما منقضی شده است. لطفاً دوباره ورو�� کنید.");
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.show();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                (message == null || message.isBlank() || "مشخص نشده".equals(message))
                        ? "عملیات ناموفق بود. لطفاً دوباره تلاش کنید." : message,
                ButtonType.OK);
        alert.setHeaderText(null);
        alert.show();
    }

    /**
     * استخراج مقدار یک فیلد از رشته JSON (رشته‌ای یا عددی)
     */
    private String extractJsonField(String source, String key) {
        Pattern stringPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = stringPattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        Pattern rawPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9.]+|true|false)");
        Matcher rawMatcher = rawPattern.matcher(source);
        if (rawMatcher.find()) {
            return rawMatcher.group(1);
        }
        return "مشخص نشده";
    }

    // ---------------------------------------------------------- رندر لیست آگهی‌ها

    private void renderAdvertisements(String responseBody, boolean isMyAdsView, String searchQuery) {
        VBox adsContainer = new VBox(15);
        adsContainer.setPadding(new Insets(20));
        adsContainer.setStyle("-fx-background-color: #160f29;");

        String headerText = isMyAdsView
                ? "آگهی‌های ثبت شده شما"
                : (cachedIsFavoritesView ? "علاقه‌مندی‌های شما" : "آگهی‌های تازه دپارتمان");

        Label lblHeader = new Label(headerText);
        lblHeader.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");
        adsContainer.getChildren().add(lblHeader);

        int dataIndex = responseBody.indexOf("\"data\"");
        String dataPart = dataIndex >= 0 ? responseBody.substring(dataIndex) : responseBody;

        Pattern objectPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = objectPattern.matcher(dataPart);

        boolean anyAdFound = false;
        while (matcher.find()) {
            String adJson = matcher.group(1);
            if (!adJson.contains("\"title\"")) {
                continue;
            }
            anyAdFound = true;
            adsContainer.getChildren().add(buildAdCard(adJson, isMyAdsView));
        }

        if (!anyAdFound) {
            Label lblEmpty = new Label(cachedIsFavoritesView
                    ? "هنوز آگهی‌ای را به علاقه‌مندی‌ها اضافه نکرده‌اید."
                    : "آگهی‌ای برای نمایش وجود ندارد.");
            lblEmpty.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
            adsContainer.getChildren().add(lblEmpty);
        }

        ScrollPane scrollPane = new ScrollPane(adsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
    }

    private Node buildAdCard(String adJson, boolean isMyAdsView) {
        String title = extractJsonField(adJson, "title");
        String description = extractJsonField(adJson, "description");
        String city = extractJsonField(adJson, "city");
        String category = extractJsonField(adJson, "category");
        String rawPrice = extractJsonField(adJson, "price");
        String owner = extractJsonField(adJson, "ownerUsername");
        String imageUrl = extractJsonField(adJson, "imageUrl");
        String adIdStr = extractJsonField(adJson, "id");
        String adStatus = extractJsonField(adJson, "status");

        long adId;
        try {
            adId = Long.parseLong(adIdStr);
        } catch (NumberFormatException e) {
            adId = -1;
        }
        final long finalAdId = adId;

        HBox card = new HBox(15);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_RIGHT);
        card.setCursor(Cursor.HAND);
        card.setStyle("-fx-background-color: #241942; -fx-background-radius: 12; -fx-border-color: #3b286b; -fx-border-radius: 12;");

        // 🖼️ تصویر آگهی (اولین تصویر در حالت چندتصویری)
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(90);
        imageView.setPreserveRatio(false);
        if (imageUrl != null && !imageUrl.isBlank() && !"مشخص نشده".equals(imageUrl)) {
            String firstImage = imageUrl.split(",")[0].trim();
            try {
                imageView.setImage(new Image(BASE_URL + firstImage, true));
            } catch (Exception ignored) {
            }
        }

        VBox textContainer = new VBox(5);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblDesc = new Label(description.length() > 80 ? description.substring(0, 80) + "..." : description);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

        Label lblCity = new Label("\ud83d\udccd " + city);
        lblCity.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

        Label lblPrice = new Label("\ud83d\udcb0 " + rawPrice + " تومان");
        lblPrice.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 13px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        textContainer.getChildren().addAll(lblTitle, lblDesc, lblCity, lblPrice);

        // 🏷️ نمایش وضعیت آگهی در بخش «آگهی‌های من»
        if (isMyAdsView) {
            Label lblStatus = new Label(statusToPersian(adStatus));
            lblStatus.setStyle("-fx-text-fill: " + statusToColor(adStatus)
                    + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");
            textContainer.getChildren().add(0, lblStatus);
        }

        // دکمه‌های عملیات
        if (isMyAdsView) {
            HBox actionsBox = new HBox(8);
            actionsBox.setAlignment(Pos.CENTER_LEFT);

            Button btnEdit = new Button("\u270f\ufe0f ویرایش");
            btnEdit.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnEdit.setOnMouseClicked(Event::consume);
            btnEdit.setOnAction(e -> openEditAdPage(finalAdId, title, description, rawPrice, city, category, imageUrl));

            Button btnDelete = new Button("\ud83d\uddd1\ufe0f حذف");
            btnDelete.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnDelete.setOnMouseClicked(Event::consume);
            btnDelete.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "این آگهی حذف شود؟", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.YES) {
                        deleteAd(finalAdId);
                    }
                });
            });

            actionsBox.getChildren().addAll(btnEdit, btnDelete);

            // ✅ دکمه فروخته شد (فقط وقتی هنوز فروخته نشده)
            if (!"SOLD".equalsIgnoreCase(adStatus)) {
                Button btnSold = new Button("\u2705 فروخته شد");
                btnSold.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
                btnSold.setOnMouseClicked(Event::consume);
                btnSold.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "آگهی به وضعیت «فروخته شده» تغییر کند؟", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            markAdAsSold(finalAdId);
                        }
                    });
                });
                actionsBox.getChildren().add(btnSold);
            }

            textContainer.getChildren().add(actionsBox);
        } else {
            // ⭐ دکمه علاقه‌مندی برای آگهی دیگران
            HBox actionsBox = new HBox(8);
            actionsBox.setAlignment(Pos.CENTER_LEFT);

            Button btnFav = new Button(cachedIsFavoritesView
                    ? "\ud83d\udc94 حذف از علاقه‌مندی‌ها"
                    : "\u2b50 افزودن به علاقه‌مندی‌ها");
            btnFav.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnFav.setOnMouseClicked(Event::consume);
            btnFav.setOnAction(e -> toggleFavorite(finalAdId));

            actionsBox.getChildren().add(btnFav);
            textContainer.getChildren().add(actionsBox);
        }

        card.getChildren().addAll(imageView, textContainer);

        card.setOnMouseClicked(e ->
                openAdDetailsPage(finalAdId, title, description, rawPrice, owner, imageUrl));

        return card;
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

    private String statusToPersian(String status) {
        if (status == null) return "";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "\ud83d\udd52 در انتظار تایید مدیر";
            case "ACTIVE":
                return "\ud83d\udfe2 فعال";
            case "REJECTED":
                return "\u274c رد شده";
            case "SOLD":
                return "\u2705 فروخته شده";
            default:
                return status;
        }
    }

    private String statusToColor(String status) {
        if (status == null) return "#b9a6df";
        switch (status.toUpperCase()) {
            case "PENDING":
                return "#ffc83b";
            case "ACTIVE":
                return "#6fdd8b";
            case "REJECTED":
                return "#ff6b6b";
            case "SOLD":
                return "#9e9e9e";
            default:
                return "#b9a6df";
        }
    }

    // ---------------------------------------------------------- ویرایش و حذف آگهی

    private void openEditAdPage(long adId, String title, String description, String price, String city, String category, String currentImageUrl) {
        VBox editBox = new VBox(12);
        editBox.setPadding(new Insets(25));
        editBox.setStyle("-fx-background-color: #160f29;");

        Label lblHeader = new Label("\u270f\ufe0f ویرایش آگهی");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblHint = new Label("پس از ویرایش، آگهی دوباره برای ��ازبینی به مدیر ارسال می‌شود.");
        lblHint.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

        String inputStyle = "-fx-background-color: #241942; -fx-text-fill: white; -fx-border-color: #3b286b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn';";
        String labelStyle = "-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';";

        Label lblFieldTitle = new Label("عنوان:");
        lblFieldTitle.setStyle(labelStyle);
        TextField txtTitle = new TextField(title);
        txtTitle.setStyle(inputStyle);

        Label lblFieldDesc = new Label("توضیحات:");
        lblFieldDesc.setStyle(labelStyle);
        TextArea txtDesc = new TextArea(description);
        txtDesc.setPrefRowCount(4);
        // ⚠️ برای TextArea باید -fx-control-inner-background هم ست شود، وگرنه ناحیه متن سفید می‌ماند
        txtDesc.setStyle(inputStyle + " -fx-control-inner-background: #241942; -fx-highlight-fill: #3b286b; -fx-highlight-text-fill: white;");

        Label lblFieldPrice = new Label("قیمت (تومان):");
        lblFieldPrice.setStyle(labelStyle);
        TextField txtPrice = new TextField(price);
        txtPrice.setStyle(inputStyle);

        Label lblFieldCity = new Label("شهر:");
        lblFieldCity.setStyle(labelStyle);
        ComboBox<String> cmbCity = new ComboBox<>();
        cmbCity.getItems().addAll("تهران", "مشهد", "اصفهان", "شیراز", "تبریز", "کرج", "اهواز", "قم", "کرمانشاه", "ارومیه", "رشت");
        if (city != null && !city.isBlank() && !cmbCity.getItems().contains(city)) {
            cmbCity.getItems().add(city);
        }
        cmbCity.setValue(city);

        Label lblFieldCategory = new Label("دسته‌بندی:");
        lblFieldCategory.setStyle(labelStyle);
        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.getItems().addAll("کالای دیجیتال", "وسایل نقلیه", "املاک", "لوازم خانگی", "مد و پوشاک", "سرگرمی و فراغت", "خدمات");
        if (category != null && !category.isBlank() && !"مشخص نشده".equals(category) && !cmbCategory.getItems().contains(category)) {
            cmbCategory.getItems().add(category);
        }
        if (category != null && !"مشخص نشده".equals(category)) {
            cmbCategory.setValue(category);
        }

        // 🖼️ مدیریت تک‌به‌تک عکس‌ها: پیش‌نمایش هر عکس + دکمه حذف زیر همان عکس
        Label lblImagesTitle = new Label("عکس‌های آگهی (برای حذف هر عکس، دکمه حذف زیر همان عکس را بزنید):");
        lblImagesTitle.setStyle(labelStyle);

        List<String> keptImageUrls = new ArrayList<>();
        if (currentImageUrl != null && !currentImageUrl.isBlank() && !"مشخص نشده".equals(currentImageUrl)) {
            for (String part : currentImageUrl.split(",")) {
                String src = part.trim();
                if (!src.isEmpty()) keptImageUrls.add(src);
            }
        }
        List<File> newImageFiles = new ArrayList<>();

        FlowPane thumbsPane = new FlowPane(10, 10);
        thumbsPane.setPadding(new Insets(5));

        final Runnable[] renderThumbs = new Runnable[1];
        renderThumbs[0] = () -> {
            thumbsPane.getChildren().clear();
            for (String src : new ArrayList<>(keptImageUrls)) {
                thumbsPane.getChildren().add(buildImageThumb(new Image(BASE_URL + src, 140, 100, false, true, true), "فعلی", () -> {
                    keptImageUrls.remove(src);
                    renderThumbs[0].run();
                }));
            }
            for (File f : new ArrayList<>(newImageFiles)) {
                thumbsPane.getChildren().add(buildImageThumb(new Image(f.toURI().toString(), 140, 100, false, true, true), "جدید", () -> {
                    newImageFiles.remove(f);
                    renderThumbs[0].run();
                }));
            }
            if (thumbsPane.getChildren().isEmpty()) {
                Label lblEmpty = new Label("این آگهی فعلاً هیچ عکسی ندارد.");
                lblEmpty.setStyle(labelStyle);
                thumbsPane.getChildren().add(lblEmpty);
            }
        };
        renderThumbs[0].run();

        Button btnAddImages = new Button("🖼️ افزودن عکس (چندانتخابی با Ctrl)");
        btnAddImages.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnAddImages.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("افزودن عکس به آگهی");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("تصاویر", "*.png", "*.jpg", "*.jpeg"));
            List<File> files = chooser.showOpenMultipleDialog(mainBorderPane.getScene().getWindow());
            if (files != null && !files.isEmpty()) {
                newImageFiles.addAll(files);
                renderThumbs[0].run();
            }
        });

        Button btnSave = new Button("💾 ذخیره تغییرات");
        btnSave.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnSave.setOnAction(ev -> {
            try {
                Double.parseDouble(txtPrice.getText().trim());
            } catch (NumberFormatException nfe) {
                showErrorAlert("لطفاً برای قیمت فقط عدد انگلیسی وارد کنید.");
                return;
            }
            String cityValue = cmbCity.getValue() == null ? "" : cmbCity.getValue();
            String categoryValue = cmbCategory.getValue() == null ? "" : cmbCategory.getValue();
            if (!newImageFiles.isEmpty()) {
                // اول عکس‌های جدید آپلود می‌شوند، بعد کنار عکس‌های باقی‌مانده ذخیره می‌شوند
                uploadEditImages(newImageFiles)
                        .thenAccept(joinedUrls -> Platform.runLater(() -> {
                            List<String> allUrls = new ArrayList<>(keptImageUrls);
                            for (String p : joinedUrls.split(",")) {
                                String src = p.trim();
                                if (!src.isEmpty()) allUrls.add(src);
                            }
                            sendEditRequest(adId, txtTitle.getText(), txtDesc.getText(), txtPrice.getText(), cityValue, categoryValue, String.join(",", allUrls), true);
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showErrorAlert("آپلود عکس‌های جدید با خطا مواجه شد: "
                                    + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                            return null;
                        });
            } else {
                // فقط عکس‌های باقی‌مانده ذخیره می‌شوند (اگر همه حذف شده باشند، آگهی بدون عکس می‌شود)
                sendEditRequest(adId, txtTitle.getText(), txtDesc.getText(), txtPrice.getText(), cityValue, categoryValue, String.join(",", keptImageUrls), true);
            }
        });

        Button btnBack = new Button("❌ انصراف");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> onLoadMyAdsClick());

        HBox actionRow = new HBox(10, btnSave, btnBack);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        editBox.getChildren().addAll(lblHeader, lblHint,
                lblFieldTitle, txtTitle,
                lblFieldDesc, txtDesc,
                lblFieldPrice, txtPrice,
                lblFieldCity, cmbCity,
                lblFieldCategory, cmbCategory,
                lblImagesTitle, btnAddImages, thumbsPane,
                actionRow);

        ScrollPane scrollPane = new ScrollPane(editBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
    }

    /** 🖼️ ساخت پیش‌نمایش کوچک عکس با برچسب و دکمه حذف ت��ی. */
    private VBox buildImageThumb(Image image, String tag, Runnable onRemove) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(140);
        iv.setFitHeight(100);
        iv.setPreserveRatio(false);

        Label lblTag = new Label(tag);
        lblTag.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");

        Button btnRemove = new Button("🗑️ حذف");
        btnRemove.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");
        btnRemove.setOnAction(e -> onRemove.run());

        VBox box = new VBox(5, iv, lblTag, btnRemove);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-radius: 10; -fx-background-radius: 10;");
        return box;
    }

    /** 📤 ارسال درخواست ویرایش آگهی؛ اگر includeImage=false باشد عکس‌ها دست‌نخورده می‌مانند. */
    private void sendEditRequest(long adId, String title, String description, String price, String city, String category, String imageUrl, boolean includeImage) {
        StringBuilder body = new StringBuilder("{");
        body.append("\"title\":\"").append(escapeJson(title)).append("\",");
        body.append("\"description\":\"").append(escapeJson(description)).append("\",");
        body.append("\"price\":\"").append(escapeJson(price)).append("\",");
        body.append("\"city\":\"").append(escapeJson(city)).append("\"");
        if (category != null && !category.isBlank()) {
            body.append(",\"category\":\"").append(escapeJson(category)).append("\"");
        }
        if (includeImage) {
            String safe = imageUrl == null ? "" : escapeJson(imageUrl);
            body.append(",\"image_url\":\"").append(safe).append("\"");
            body.append(",\"imageUrl\":\"").append(safe).append("\"");
        }
        body.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        showSuccessAlert("آگهی ویرایش شد و برای بازبینی مجدد به مدیر ارسال گردید.");
                        onLoadMyAdsClick();
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    /** 📡 آپلود چند عکس برای ویرایش آگهی در یک درخواست Multipart (کلید files). */
    private CompletableFuture<String> uploadEditImages(List<File> files) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            String boundary = "JavaFX-Boundary-" + UUID.randomUUID();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (File file : files) {
                String mimeType = "image/jpeg";
                try {
                    String probed = Files.probeContentType(file.toPath());
                    if (probed != null) mimeType = probed;
                } catch (IOException ignored) {
                }
                String header = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"files\"; filename=\"" + file.getName() + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n\r\n";
                out.write(header.getBytes(StandardCharsets.UTF_8));
                out.write(Files.readAllBytes(file.toPath()));
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/upload"))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            String joined = extractJsonField(response.body(), "image_url");
                            if (joined != null && !joined.isBlank() && !"مشخص نشده".equals(joined)) {
                                future.complete(joined);
                            } else {
                                future.completeExceptionally(new RuntimeException("آدرس تصویر در پاسخ سرور یافت نشد."));
                            }
                        } else {
                            future.completeExceptionally(new RuntimeException("کد وضعیت سرور: " + response.statusCode()));
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

    private void deleteAd(long adId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .DELETE()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        showSuccessAlert("آگهی با موفقیت حذف شد.");
                        onLoadMyAdsClick();
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    // ---------------------------------------------------------- صفحه جزئیات آگهی

    private void openAdDetailsPage(long adId, String title, String description,
                                   String rawPrice, String owner, String imageUrl) {
        VBox detailsBox = new VBox(15);
        detailsBox.setPadding(new Insets(25));
        detailsBox.setStyle("-fx-background-color: #160f29;");

        Button btnBack = new Button("\u2b05 بازگشت");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> renderAdvertisements(cachedAdsJson, cachedIsMyAdsView, ""));

        // 🖼️ گالری تصاویر (امتیازی: چند تصویر + جابجایی بین عکس‌ها با دکمه‌های قبلی/بعدی)
        VBox gallery = new VBox(8);
        gallery.setAlignment(Pos.CENTER);
        List<String> imageSources = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isBlank() && !"مشخص نشده".equals(imageUrl)) {
            for (String part : imageUrl.split(",")) {
                String src = part.trim();
                if (!src.isEmpty()) imageSources.add(src);
            }
        }
        if (!imageSources.isEmpty()) {
            ImageView imageViewGallery = new ImageView();
            imageViewGallery.setFitWidth(420);
            imageViewGallery.setFitHeight(280);
            imageViewGallery.setPreserveRatio(true);

            Label lblCounter = new Label();
            lblCounter.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

            final int[] currentIndex = {0};
            Runnable showCurrentImage = () -> {
                try {
                    imageViewGallery.setImage(new Image(BASE_URL + imageSources.get(currentIndex[0]), true));
                } catch (Exception ignored) {
                }
                lblCounter.setText((currentIndex[0] + 1) + " / " + imageSources.size());
            };
            showCurrentImage.run();

            String navBtnStyle = "-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';";

            Button btnPrevImage = new Button("قبلی ▶");
            btnPrevImage.setStyle(navBtnStyle);
            btnPrevImage.setOnAction(e -> {
                currentIndex[0] = (currentIndex[0] - 1 + imageSources.size()) % imageSources.size();
                showCurrentImage.run();
            });

            Button btnNextImage = new Button("◀ بعدی");
            btnNextImage.setStyle(navBtnStyle);
            btnNextImage.setOnAction(e -> {
                currentIndex[0] = (currentIndex[0] + 1) % imageSources.size();
                showCurrentImage.run();
            });

            gallery.getChildren().add(imageViewGallery);

            // دکمه‌های جابجایی فقط وقتی بیش از یک عکس وجود دارد
            if (imageSources.size() > 1) {
                HBox navRow = new HBox(15, btnNextImage, lblCounter, btnPrevImage);
                navRow.setAlignment(Pos.CENTER);
                gallery.getChildren().add(navRow);
            }
        }

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblPrice = new Label("\ud83d\udcb0 قیمت: " + rawPrice + " تومان");
        lblPrice.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblOwner = new Label("\ud83d\udc64 فروشنده: " + owner);
        lblOwner.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        // ⭐ امتیاز فروشنده
        Label lblRating = new Label("\u2b50 امتیاز فروشنده: در حال دریافت...");
        lblRating.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");
        loadSellerRating(adId, lblRating);

        Separator separator = new Separator();

        Label lblDescTitle = new Label("\ud83d\udcdd توضیحات:");
        lblDescTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        boolean isOwnAd = owner != null && owner.equals(MainApplication.currentUsername);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        if (!isOwnAd) {
            Button btnStartChat = new Button("\ud83d\udcac گفتگو با فروشنده");
            btnStartChat.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnStartChat.setOnAction(e -> openChatWithUser(adId, owner, title));

            Button btnRate = new Button("\u2b50 ثبت امتیاز به فروشنده");
            btnRate.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnRate.setOnAction(e -> openRatingDialog(adId, lblRating));

            actionRow.getChildren().addAll(btnStartChat, btnRate);
        }

        detailsBox.getChildren().addAll(btnBack, gallery, lblTitle, lblPrice, lblOwner,
                lblRating, separator, lblDescTitle, lblDesc, actionRow);

        ScrollPane scrollPane = new ScrollPane(detailsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
    }

    // ---------------------------------------------------------- ⭐ امتیاز فروشنده

    private void loadSellerRating(long adId, Label target) {
        client.sendAsync(authorizedGet(BASE_URL + "/api/ratings/ad/" + adId), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        String average = extractJsonField(response.body(), "average");
                        String count = extractJsonField(response.body(), "count");
                        if ("0".equals(count) || "مشخص نشده".equals(count)) {
                            target.setText("\u2b50 امتیاز فروشنده: بدون امتیاز");
                        } else {
                            target.setText("\u2b50 امتیاز فرو��نده: " + average + " (از " + count + " رای)");
                        }
                    } else {
                        target.setText("\u2b50 امتیاز فروشنده: نامشخص");
                    }
                }));
    }

    private void openRatingDialog(long adId, Label ratingLabel) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(5, List.of(1, 2, 3, 4, 5));
        dialog.setTitle("ثبت امتیاز");
        dialog.setHeaderText("امتیاز شما به فروشنده (۱ تا ۵):");
        dialog.setContentText("امتیاز:");

        dialog.showAndWait().ifPresent(score -> {
            String body = "{\"adId\":" + adId + ",\"score\":" + score + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/ratings"))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        checkAndRefreshToken(response);
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            showSuccessAlert("امتیاز شما با موفقیت ثبت شد.");
                            loadSellerRating(adId, ratingLabel);
                        } else if (response.statusCode() == 401) {
                            handleUnauthorized(response.statusCode());
                        } else {
                            showErrorAlert(extractJsonField(response.body(), "message"));
                        }
                    }));
        });
    }

    // ---------------------------------------------------------- 💬 چت

    private void openChatWithUser(long adId, String sellerUsername, String adTitle) {
        String body = "{\"adId\":" + adId + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/start"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        String convIdStr = extractJsonField(response.body(), "conversationId");
                        try {
                            openChatPane(Long.parseLong(convIdStr), sellerUsername, adTitle);
                        } catch (NumberFormatException e) {
                            showErrorAlert("خطا در شروع گفتگو.");
                        }
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private void openChatPane(long conversationId, String otherUser, String adTitle) {
        setActiveTopBarSection("chat");
        VBox messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(15));
        messagesBox.setStyle("-fx-background-color: #160f29;");

        ScrollPane scroll = new ScrollPane(messagesBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");

        Label lblHeader = new Label("\ud83d\udcac گفتگو با " + otherUser + " — " + adTitle);
        lblHeader.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 15px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Button btnBack = new Button("\u2b05 لیست گفتگوها");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> onChatScreenClick());

        Button btnHome = new Button("\ud83c\udfe0 صفحه اصلی");
        btnHome.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnHome.setOnAction(e -> showHomeScreen());

        Button btnRefresh = new Button("\ud83d\udd04");
        btnRefresh.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> loadChatMessages(conversationId, messagesBox));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, lblHeader, spacer, btnRefresh, btnHome, btnBack);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #241942;");

        TextField input = new TextField();
        input.setPromptText("پیام خود را بنویسید...");
        input.setStyle("-fx-background-color: #241942; -fx-text-fill: white; -fx-prompt-text-fill: #b9a6df; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn';");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button btnSend = new Button("\ud83d\udce4 ارسال");
        btnSend.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");

        Runnable sendAction = () -> {
            String text = input.getText() == null ? "" : input.getText().trim();
            if (text.isEmpty()) return;

            String body = "{\"content\":\"" + escapeJson(text) + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/chat/conversations/" + conversationId + "/messages"))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        checkAndRefreshToken(response);
                        if (response.statusCode() == 200) {
                            input.clear();
                            loadChatMessages(conversationId, messagesBox);
                        } else if (response.statusCode() == 401) {
                            handleUnauthorized(response.statusCode());
                        } else {
                            showErrorAlert(extractJsonField(response.body(), "message"));
                        }
                    }));
        };

        btnSend.setOnAction(e -> sendAction.run());
        input.setOnAction(e -> sendAction.run());

        HBox inputRow = new HBox(10, input, btnSend);
        inputRow.setPadding(new Insets(15));
        inputRow.setStyle("-fx-background-color: #241942;");

        BorderPane chatRoot = new BorderPane();
        chatRoot.setTop(header);
        chatRoot.setCenter(scroll);
        chatRoot.setBottom(inputRow);
        chatRoot.setStyle("-fx-background-color: #160f29;");

        mainBorderPane.setCenter(chatRoot);
        loadChatMessages(conversationId, messagesBox);
    }

    private void loadChatMessages(long conversationId, VBox messagesBox) {
        client.sendAsync(authorizedGet(BASE_URL + "/api/chat/conversations/" + conversationId + "/messages"),
                        HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() != 200) {
                        return;
                    }
                    messagesBox.getChildren().clear();

                    int dataIndex = response.body().indexOf("\"data\"");
                    String dataPart = dataIndex >= 0 ? response.body().substring(dataIndex) : response.body();

                    Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(dataPart);
                    while (matcher.find()) {
                        String msgJson = matcher.group(1);
                        if (!msgJson.contains("\"content\"")) continue;

                        String sender = extractJsonField(msgJson, "senderUsername");
                        String content = extractJsonField(msgJson, "content");
                        boolean mine = sender.equals(MainApplication.currentUsername);

                        Label bubble = new Label(content);
                        bubble.setWrapText(true);
                        bubble.setMaxWidth(420);
                        bubble.setPadding(new Insets(8, 12, 8, 12));
                        // 💬 پیام من: سمت راست (بنفش) — پیام مخاطب: سمت چپ (طلایی)
                        bubble.setStyle(mine
                                ? "-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-family: 'Vazirmatn';"
                                : "-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-background-radius: 12; -fx-font-family: 'Vazirmatn';");

                        HBox row = new HBox(bubble);
                        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        messagesBox.getChildren().add(row);
                    }
                }));
    }

    // ---------------------------------------------------------- 🛡️ پنل مدیریت و نقش کاربر

    /**
     * 🔑 استخراج نام کاربری واقعی از payload توکن JWT (claim «sub»)
     */
    private String extractUsernameFromToken() {
        try {
            String token = MainApplication.jwtToken;
            if (token == null || token.isBlank()) return "";
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * استخراج نقش کاربر از payload توکن JWT (claim «role»)
     */
    private String extractRoleFromToken() {
        try {
            String token = MainApplication.jwtToken;
            if (token == null || token.isBlank()) return "";
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "";
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("\"role\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return payload.contains("ADMIN") ? "ADMIN" : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void openAdminPanel() {
        setActiveTopBarSection("myDivar");
        mainBorderPane.setCenter(new AdminPanelController(this::showHomeScreen).createView());
    }

    // ---------------------------------------------------------- ثبت آگهی و خروج

    @FXML
    protected void onRegisterAdScreenClick() {
        setActiveTopBarSection("register");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register-ad-view.fxml"));
            Parent view = loader.load();
            RegisterAdController registerController = loader.getController();
            if (registerController != null) {
                registerController.setHelloController(this);
            }
            mainBorderPane.setCenter(view);
        } catch (Exception e) {
            showErrorAlert("خطا در باز کردن صفحه ثبت آگهی.");
        }
    }

    @FXML
    protected void onLogoutClick() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/logout"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    Stage stage = (Stage) mainBorderPane.getScene().getWindow();
                    MainApplication.jwtToken = "";
                    MainApplication.redirectToLogin(stage, "با موفقیت از حساب خارج شدید.");
                }));
    }
}
