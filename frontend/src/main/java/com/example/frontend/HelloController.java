package com.example.frontend;

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
    /** ⭐ شناسه آگهی‌های موجود در علاقه‌مندی‌های کاربر */
    private final java.util.Set<Long> favoriteAdIds = new java.util.HashSet<>();

    // 🔍 کنترل‌های فیلتر و مرتب‌سازی در ردیف بالا
    @FXML private ComboBox<String> cityFilterCombo;
    @FXML private ComboBox<String> sortFilterCombo;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;

    // 🔍 فیلترهای ترکیبی (امتیازی)
    private String filterCategory = "";

    // دسته‌های اصلی بازشده در ستون دسته‌بندی (حفظ وضعیت بین بازسازی‌ها)
    private final java.util.Set<String> expandedCategories = new java.util.HashSet<>();

    // 🗂️ دسته‌بندی‌های دریافت‌شده از سرور
    private final java.util.List<String> serverCategories = new java.util.ArrayList<>();

    // نگاشت دسته اصلی ← زیردسته‌ها (برای فرم ویرایش آگهی)
    private final java.util.Map<String, java.util.List<String>> serverCategoryTree = new java.util.LinkedHashMap<>();

    // شهرهای دریافتی از سرور (قابل مدیریت توسط ادمین)
    private final java.util.List<String> serverCities = new java.util.ArrayList<>(java.util.List.of(
            "تهران", "مشهد", "اصفهان", "شیراز", "تبریز", "کرج", "اهواز", "قم", "کرمانشاه", "ارومیه", "رشت"));
    private String filterCity = "";
    private String filterMinPrice = "";
    private String filterMaxPrice = "";
    private String filterSort = "newest";

    // ---------------------------------------------------------- شروع

    @FXML
    public void initialize() {
        // 🔑 اصلاح مهم: نام کاربری واقعی از توکن JWT خوانده می‌شود.
        // پاسخ لاگین فیلد username ندارد و LoginController شماره تلفن را ذخیره می‌کرد؛
        // در نتیجه مقایسه فرستنده پیام با کاربر جاری همیشه شکست می‌خورد و همه پیام‌ها سمت چپ می‌افتادند.
        String tokenUsername = extractUsernameFromToken();
        if (!tokenUsername.isBlank()) {
            MainApplication.currentUsername = tokenUsername;
        }

        hoverMenu.setStyle("-fx-background-color: #241942; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-width: 1; -fx-border-radius: 10; -fx-padding: 6; -fx-selection-bar: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        CustomMenuItem profileItem = buildThemedMenuItem("\ud83d\udc64 آگهی‌های من", this::onLoadMyAdsClick);
        CustomMenuItem favoritesItem = buildThemedMenuItem("\u2764 علاقه‌مندی‌های من", this::showFavoritesScreen);
        CustomMenuItem logoutItem = buildThemedMenuItem("\ud83d\udeaa خروج از حساب", this::onLogoutClick);

        CustomMenuItem editProfileItem = buildThemedMenuItem("⚙️ ویرایش مشخصات", this::openProfileEditPage);

        hoverMenu.getItems().addAll(profileItem, favoritesItem, editProfileItem);

        // 🛡️ فقط برای مدیر سیستم
        if ("ADMIN".equalsIgnoreCase(extractRoleFromToken())) {
            hoverMenu.getItems().add(buildThemedMenuItem("\ud83d\udee1\ufe0f پنل مدیریت", this::openAdminPanel));
        }

        hoverMenu.getItems().add(logoutItem);

        if (myDivarButton != null) {
            // باز شدن پنل «بازارچه من» با رفتن موس روی دکمه
            javafx.animation.PauseTransition hideDelay =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(250));
            hideDelay.setOnFinished(ev -> hoverMenu.hide());

            myDivarButton.setOnMouseEntered(e -> {
                hideDelay.stop();
                if (!hoverMenu.isShowing()) {
                    hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 5);
                }
            });
            myDivarButton.setOnMouseExited(e -> hideDelay.playFromStart());

            // تا وقتی موس روی خود منو است، منو باز بماند
            hoverMenu.setOnShown(ev -> {
                Node menuRoot = hoverMenu.getScene().getRoot();
                menuRoot.setOnMouseEntered(e2 -> hideDelay.stop());
                menuRoot.setOnMouseExited(e2 -> hideDelay.playFromStart());
            });

            myDivarButton.setOnMouseClicked(e ->
                    hoverMenu.show(myDivarButton, Side.BOTTOM, 0, 5));
        }

        if (searchField != null) {
            searchField.setOnAction(e -> refreshCurrentListView());
        }

        // 🔍 مقداردهی کنترل‌های فیلتر و مرتب‌سازی ردیف بالا
        if (cityFilterCombo != null) {
            cityFilterCombo.getItems().add("همه شهرها");
            cityFilterCombo.getItems().addAll(serverCities);
        }
        loadCitiesFromServer();
        if (sortFilterCombo != null) {
            sortFilterCombo.getItems().addAll("جدیدترین", "ارزان‌ترین", "گران‌ترین");
        }

        // 🎨 هماهنگ‌سازی رنگ کمبوباکس‌های فیلتر/مرتب‌سازی با تم برنامه
        if (cityFilterCombo != null) styleComboBox(cityFilterCombo);
        if (sortFilterCombo != null) styleComboBox(sortFilterCombo);

        setActiveTopBarSection("home");
        updateCategoryButtonStyles(null);
        showHomeScreen();
    }

    private CustomMenuItem buildThemedMenuItem(String text, Runnable action) {
        Label lbl = new Label(text);
        final String normalStyle = "-fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px; -fx-padding: 8 14 8 14; -fx-background-color: transparent; -fx-background-radius: 6;";
        final String hoverStyle = "-fx-text-fill: #ffc83b; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px; -fx-padding: 8 14 8 14; -fx-background-color: #3b286b; -fx-background-radius: 6;";
        lbl.setStyle(normalStyle);
        lbl.setMinWidth(180);
        lbl.setPrefWidth(210);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setCursor(Cursor.HAND);
        lbl.setOnMouseEntered(e -> lbl.setStyle(hoverStyle));
        lbl.setOnMouseExited(e -> lbl.setStyle(normalStyle));
        lbl.setOnMouseClicked(e -> action.run());
        CustomMenuItem item = new CustomMenuItem(lbl, true);
        item.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-selection-bar: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 0;");
        return item;
    }

    public static void styleComboBox(ComboBox<String> combo) {
        final String btnStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-padding: 0 6 0 6;";
        ListCell<String> btnCell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? combo.getPromptText() : item);
            }
        };
        btnCell.setStyle(btnStyle);
        combo.setButtonCell(btnCell);

        combo.setCellFactory(listView -> {
            listView.setStyle("-fx-background-color: #241942; -fx-background-insets: 0; -fx-padding: 0; -fx-border-color: #3b286b; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            final String normalCell = "-fx-background-color: #241942; -fx-background-insets: 0; -fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn'; -fx-padding: 7 12 7 12; -fx-cursor: hand;";
            final String hoverCell = "-fx-background-color: #3b286b; -fx-background-insets: 0; -fx-text-fill: #ffc83b; -fx-font-family: 'Vazirmatn'; -fx-padding: 7 12 7 12; -fx-cursor: hand;";
            cell.setStyle(normalCell);
            cell.setOnMouseEntered(e -> cell.setStyle(hoverCell));
            cell.setOnMouseExited(e -> cell.setStyle(normalCell));
            return cell;
        });
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

    // 🗂 بارگذاری پویای دسته‌بندی‌ها از سرور (شامل دسته‌های جدیدی که ادمین اضافه می‌کند)
    private void loadCategoriesIntoSidebar() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/categories"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) return;

                    // تجزیه دسته‌بندی‌ها همراه با شناسه و والد (برای زیردسته‌بندی)
                    java.util.List<Long> catIds = new java.util.ArrayList<>();
                    java.util.List<String> catNames = new java.util.ArrayList<>();
                    java.util.List<Long> catParents = new java.util.ArrayList<>();
                    java.util.regex.Matcher obj = java.util.regex.Pattern
                            .compile("\\{[^{}]*\\}")
                            .matcher(response.body());
                    while (obj.find()) {
                        String o = obj.group();
                        java.util.regex.Matcher mi = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(o);
                        java.util.regex.Matcher mn = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(o);
                        java.util.regex.Matcher mp = java.util.regex.Pattern.compile("\"parentId\"\\s*:\\s*(\\d+)").matcher(o);
                        if (!mi.find() || !mn.find()) continue;
                        catIds.add(Long.parseLong(mi.group(1)));
                        catNames.add(mn.group(1).trim());
                        catParents.add(mp.find() ? Long.parseLong(mp.group(1)) : -1L);
                    }

                    // ترتیب نمایش: هر دسته اصلی و زیردسته‌های آن (ساختار درختی)
                    java.util.List<String> names = new java.util.ArrayList<>();
                    java.util.Map<String, java.util.List<String>> catTree = new java.util.LinkedHashMap<>();
                    for (int i = 0; i < catNames.size(); i++) {
                        if (catParents.get(i) >= 0 && catIds.contains(catParents.get(i))) continue;
                        String parentName = catNames.get(i);
                        if (parentName.isEmpty() || names.contains(parentName)) continue;
                        names.add(parentName);
                        java.util.List<String> kids = new java.util.ArrayList<>();
                        long pid = catIds.get(i);
                        for (int j = 0; j < catNames.size(); j++) {
                            String childName = catNames.get(j);
                            if (catParents.get(j) == pid && !childName.isEmpty() && !names.contains(childName)) {
                                names.add(childName);
                                kids.add(childName);
                            }
                        }
                        catTree.put(parentName, kids);
                    }
                    if (names.isEmpty()) return;

                    Platform.runLater(() -> {
                        serverCategories.clear();
                        serverCategories.addAll(names);
                        serverCategoryTree.clear();
                        serverCategoryTree.putAll(catTree);
                        if (categoryVBox == null) return;

                        // حذف آیتم‌های قبلی به‌جز «همه محصولات»
                        categoryVBox.getChildren().removeIf(node ->
                                (node instanceof Button && !"همه محصولات".equals(((Button) node).getText()))
                                        || node instanceof HBox);

                        String catBtnStyle = "-fx-background-color: transparent; -fx-text-fill: #b9a6df; -fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
                        for (java.util.Map.Entry<String, java.util.List<String>> entry : catTree.entrySet()) {
                            String name = entry.getKey();
                            if ("همه محصولات".equals(name)) continue;
                            java.util.List<String> kids = entry.getValue();

                            Button btn = new Button(name);
                            btn.setAlignment(javafx.geometry.Pos.BASELINE_RIGHT);
                            btn.setMaxWidth(Double.MAX_VALUE);
                            btn.setStyle(catBtnStyle);
                            btn.setOnAction(this::onCategoryClick);

                            if (kids.isEmpty()) {
                                // دسته بدون زیردسته: دکمه ساده
                                categoryVBox.getChildren().add(btn);
                                continue;
                            }

                            // دسته دارای زیردسته: فلش بازشو کنار نام دسته اصلی
                            if (!filterCategory.isBlank() && kids.contains(filterCategory)) {
                                expandedCategories.add(name); // زیردسته فعال باید دیده شود
                            }
                            boolean expanded = expandedCategories.contains(name);

                            Label arrow = new Label(expanded ? "\u25be" : "\u25c2");
                            arrow.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");

                            HBox parentRow = new HBox(2, arrow, btn);
                            parentRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                            HBox.setHgrow(btn, Priority.ALWAYS);
                            categoryVBox.getChildren().add(parentRow);

                            java.util.List<Button> childBtns = new java.util.ArrayList<>();
                            for (String childName : kids) {
                                Button childBtn = new Button(childName);
                                childBtn.setAlignment(javafx.geometry.Pos.BASELINE_RIGHT);
                                childBtn.setMaxWidth(Double.MAX_VALUE);
                                childBtn.setStyle(catBtnStyle);
                                childBtn.setPadding(new javafx.geometry.Insets(2, 22, 2, 2));
                                childBtn.setOnAction(this::onCategoryClick);
                                childBtn.setVisible(expanded);
                                childBtn.setManaged(expanded);
                                categoryVBox.getChildren().add(childBtn);
                                childBtns.add(childBtn);
                            }

                            // کلیک روی فلش = باز/بسته کردن زیردسته‌ها
                            arrow.setOnMouseClicked(ev -> {
                                boolean open = !expandedCategories.contains(name);
                                if (open) expandedCategories.add(name); else expandedCategories.remove(name);
                                arrow.setText(open ? "\u25be" : "\u25c2");
                                for (Button cb : childBtns) {
                                    cb.setVisible(open);
                                    cb.setManaged(open);
                                }
                            });
                        }

                        // حفظ هایلایت دسته فعال پس از بازسازی لیست
                        Button activeBtn = null;
                        for (Button b : getCategorySidebarButtons()) {
                            if (!filterCategory.isBlank() && filterCategory.equals(b.getText())) {
                                activeBtn = b;
                                break;
                            }
                        }
                        // اگر دسته فعال توسط ادمین حذف شده باشد، فیلتر پاک و لیست تازه می‌شود
                        if (!filterCategory.isBlank() && activeBtn == null) {
                            filterCategory = "";
                            refreshCurrentListView();
                        }
                        updateCategoryButtonStyles(activeBtn);
                    });
                });
    }

    /** 🔶 به‌روزکردن کادر دسته‌بندی فعال در ستون سمت راست */
    private void updateCategoryButtonStyles(Button activeBtn) {
        if (categoryVBox == null) return;
        String normal = "-fx-background-color: transparent; -fx-text-fill: #b9a6df; -fx-cursor: hand; -fx-border-radius: 6; -fx-background-radius: 6;";
        String active = "-fx-background-color: transparent; -fx-text-fill: #ffc83b; -fx-cursor: hand; -fx-border-color: #ffc83b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-width: 1.5;";
        for (Button b : getCategorySidebarButtons()) {
            b.setStyle(normal);
        }
        if (activeBtn == null) {
            for (Button b : getCategorySidebarButtons()) {
                if ("همه محصولات".equals(b.getText())) {
                    activeBtn = b;
                    break;
                }
            }
        }
        if (activeBtn != null) activeBtn.setStyle(active);
    }

    /** همه دکمه‌های دسته‌بندی ستون راست (شامل دکمه‌های داخل ردیف‌های فلش‌دار) */
    private java.util.List<Button> getCategorySidebarButtons() {
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        if (categoryVBox == null) return buttons;
        for (javafx.scene.Node node : categoryVBox.getChildren()) {
            if (node instanceof Button) {
                buttons.add((Button) node);
            } else if (node instanceof HBox) {
                for (javafx.scene.Node inner : ((HBox) node).getChildren()) {
                    if (inner instanceof Button) buttons.add((Button) inner);
                }
            }
        }
        return buttons;
    }

    /** 🙈 نمایش/مخفی‌سازی ستون دسته‌بندی‌ها (در چت، ثبت آگهی و پنل مدیریت مخفی می‌شود) */
    private void setCategorySidebarVisible(boolean visible) {
        if (mainBorderPane == null || categoryVBox == null) return;
        mainBorderPane.setRight(visible ? categoryVBox : null);
    }

    public void showHomeScreen() {
        setActiveTopBarSection("home");
        setCategorySidebarVisible(true);
        loadCategoriesIntoSidebar();
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
        setCategorySidebarVisible(false);
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
        VBox listBox = new VBox(12);
        listBox.setPadding(new Insets(25));
        listBox.setStyle("-fx-background-color: #160f29;");

        Label lblHeader = new Label("\ud83d\udcac گفتگوهای شما");
        lblHeader.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        // نشان تعداد گفتگوها کنار عنوان
        Label lblCount = new Label();
        lblCount.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 12; -fx-padding: 3 10 3 10; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

        Button btnHomeList = new Button("\ud83c\udfe0 صفحه اصلی");
        btnHomeList.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 14 8 14; -fx-font-family: 'Vazirmatn';");
        btnHomeList.setOnAction(e -> showHomeScreen());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, lblHeader, lblCount, headerSpacer, btnHomeList);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        listBox.getChildren().add(headerRow);

        // خط جداکننده زیر هدر
        Region headerDivider = new Region();
        headerDivider.setPrefHeight(1);
        headerDivider.setStyle("-fx-background-color: #3b286b;");
        listBox.getChildren().add(headerDivider);

        int dataIndex = responseBody.indexOf("\"data\"");
        String dataPart = dataIndex >= 0 ? responseBody.substring(dataIndex) : responseBody;

        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(dataPart);
        int convCount = 0;
        while (matcher.find()) {
            String convJson = matcher.group(1);
            if (!convJson.contains("\"buyerUsername\"")) continue;
            convCount++;

            String convIdStr = extractJsonField(convJson, "id");
            String buyer = extractJsonField(convJson, "buyerUsername");
            String seller = extractJsonField(convJson, "sellerUsername");
            String adTitle = extractJsonField(convJson, "adTitle");
            String otherUser = buyer.equals(MainApplication.currentUsername) ? seller : buyer;
            String lastMsgContent = extractJsonField(convJson, "lastMessageContent");
            String lastMsgSender  = extractJsonField(convJson, "lastMessageSender");

            // آواتار دایره‌ای با حرف اول نام مخاطب
            Label avatar = new Label(otherUser.isEmpty() ? "?" : otherUser.substring(0, 1).toUpperCase());
            avatar.setMinSize(44, 44);
            avatar.setMaxSize(44, 44);
            avatar.setAlignment(Pos.CENTER);
            avatar.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 22; -fx-font-family: 'Vazirmatn';");

            Label lblName = new Label(otherUser);
            lblName.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

            Label lblAd = new Label("\ud83c\udff7\ufe0f درباره آگهی: " + adTitle);
            lblAd.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

            VBox infoBox;
            if (!lastMsgContent.isEmpty()) {
                String senderLabel = lastMsgSender.equals(MainApplication.currentUsername)
                    ? "شما"
                    : lastMsgSender;
                String preview = lastMsgContent.length() > 38
                    ? lastMsgContent.substring(0, 38) + "..."
                    : lastMsgContent;
                Label lblLastMsg = new Label(senderLabel + ": " + preview);
                lblLastMsg.setStyle("-fx-text-fill: #8b7ca6; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn'; -fx-font-style: italic;");
                lblLastMsg.setMaxWidth(260);
                infoBox = new VBox(3, lblName, lblAd, lblLastMsg);
            } else {
                infoBox = new VBox(4, lblName, lblAd);
            }
            infoBox.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label chevron = new Label("\u276e");
            chevron.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 16px;");

            HBox card = new HBox(12, avatar, infoBox, spacer, chevron);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(12, 16, 12, 16));
            String cardNormal = "-fx-background-color: #241942; -fx-background-radius: 14; -fx-border-color: #3b286b; -fx-border-radius: 14; -fx-cursor: hand;";
            String cardHover = "-fx-background-color: #2b1f4e; -fx-background-radius: 14; -fx-border-color: #ffc83b; -fx-border-radius: 14; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255,200,59,0.35), 16, 0.2, 0, 2);";
            card.setStyle(cardNormal);
            card.setOnMouseEntered(e -> card.setStyle(cardHover));
            card.setOnMouseExited(e -> card.setStyle(cardNormal));
            card.setOnMouseClicked(e -> {
                try {
                    openChatPane(Long.parseLong(convIdStr), 0L, otherUser, adTitle);
                } catch (NumberFormatException ignored) {
                }
            });
            listBox.getChildren().add(card);
        }
        lblCount.setText(convCount + " گفتگو");

        if (convCount == 0) {
            // حالت خالی: کارت راهنمای وسط‌چین
            Label lblEmptyIcon = new Label("\ud83d\udcac");
            lblEmptyIcon.setStyle("-fx-font-size: 42px;");
            Label lblEmpty = new Label("هنوز گفتگویی ندارید");
            lblEmpty.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");
            Label lblEmptyHint = new Label("از صفحه جزئیات هر آگهی می‌توانید با فروشنده گفتگو را شروع کنید.");
            lblEmptyHint.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");
            VBox emptyBox = new VBox(8, lblEmptyIcon, lblEmpty, lblEmptyHint);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            emptyBox.setStyle("-fx-background-color: #241942; -fx-background-radius: 14; -fx-border-color: #3b286b; -fx-border-radius: 14;");
            listBox.getChildren().add(emptyBox);
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
                        refreshFavoriteIds(() -> renderAdvertisements(cachedAdsJson, false, searchQuery));
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
        setCategorySidebarVisible(true);
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
        setCategorySidebarVisible(true);
        client.sendAsync(authorizedGet(BASE_URL + "/api/favorites"), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        cachedAdsJson = response.body();
                        cachedIsMyAdsView = false;
                        cachedIsFavoritesView = true;
                        favoriteAdIds.clear();
                        java.util.regex.Matcher fm = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(response.body());
                        while (fm.find()) favoriteAdIds.add(Long.parseLong(fm.group(1)));
                        renderAdvertisements(response.body(), false, "");
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    /** دریافت شناسه آگهی‌های علاقه‌مندی برای همگام‌سازی دکمه‌ها */
    private void refreshFavoriteIds(Runnable onDone) {
        client.sendAsync(authorizedGet(BASE_URL + "/api/favorites"), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        favoriteAdIds.clear();
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(response.body());
                        while (m.find()) favoriteAdIds.add(Long.parseLong(m.group(1)));
                    }
                    if (onDone != null) onDone.run();
                }))
                .exceptionally(ex -> {
                    if (onDone != null) Platform.runLater(onDone);
                    return null;
                });
    }

    private void toggleFavorite(long adId, Button sourceBtn) {
        boolean removing = cachedIsFavoritesView || favoriteAdIds.contains(adId);

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
                            favoriteAdIds.remove(adId);
                            if (cachedIsFavoritesView) {
                                showFavoritesScreen();
                            } else {
                                if (sourceBtn != null) sourceBtn.setText("\u2764 افزودن به علاقه‌مندی‌ها");
                                showSuccessAlert("آگهی از علاقه‌مندی‌های شما حذف شد.");
                            }
                        } else {
                            favoriteAdIds.add(adId);
                            if (sourceBtn != null) sourceBtn.setText("\ud83d\udc94 حذف از علاقه‌مندی‌ها");
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
        MainApplication.redirectToLogin(stage, "نشست شما منقضی شده است. لطفاً دوباره ورود کنید.");
    }

    /** ✅ اعلان موفقیت به‌صورت اعلان کوتاه (بدون پاپ‌آپ). */
    private void showSuccessAlert(String message) {
        UiTheme.toast(message);
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                (message == null || message.isBlank() || "مشخص نشده".equals(message))
                        ? "عملیات ناموفق بود. لطفاً دوباره تلاش کنید." : message,
                ButtonType.OK);
        alert.setHeaderText(null);
        UiTheme.styleAlert(alert);
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

        // چیدمان دو ستونه کارت‌ها (هر ردیف دو کالا)
        GridPane cardsGrid = new GridPane();
        cardsGrid.setHgap(15);
        cardsGrid.setVgap(15);
        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setPercentWidth(50);
        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setPercentWidth(50);
        cardsGrid.getColumnConstraints().addAll(colRight, colLeft);

        boolean anyAdFound = false;
        int cardIndex = 0;
        while (matcher.find()) {
            String adJson = matcher.group(1);
            if (!adJson.contains("\"title\"")) {
                continue;
            }
            anyAdFound = true;
            Node cardNode = buildAdCard(adJson, isMyAdsView);
            GridPane.setHgrow(cardNode, Priority.ALWAYS);
            GridPane.setFillWidth(cardNode, true);
            cardsGrid.add(cardNode, cardIndex % 2, cardIndex / 2);
            cardIndex++;
        }

        if (anyAdFound) {
            adsContainer.getChildren().add(cardsGrid);
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
        card.setMaxWidth(Double.MAX_VALUE);

        // نور طلایی زیر کارت هنگام رفتن موس روی آن
        final String cardNormalStyle = "-fx-background-color: #241942; -fx-background-radius: 12; -fx-border-color: #3b286b; -fx-border-radius: 12;";
        final String cardHoverStyle = "-fx-background-color: #241942; -fx-background-radius: 12; -fx-border-color: #ffc83b; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(255, 200, 59, 0.75), 20, 0.35, 0, 8);";
        card.setStyle(cardNormalStyle);
        card.setOnMouseEntered(e -> card.setStyle(cardHoverStyle));
        card.setOnMouseExited(e -> card.setStyle(cardNormalStyle));

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
            // با کلیک روی عکس در همین صفحه، عکس بزرگ نمایش داده می‌شود
            imageView.setCursor(Cursor.HAND);
            imageView.setOnMouseClicked(e -> {
                e.consume();
                showEnlargedImage(BASE_URL + firstImage);
            });
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
                ButtonType yesBt = new ButtonType("بله، حذف شود", javafx.scene.control.ButtonBar.ButtonData.YES);
                ButtonType noBt = new ButtonType("انصراف", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "این آگهی حذف شود؟", yesBt, noBt);
                confirm.setHeaderText(null);
                confirm.setTitle("تایید حذف آگهی");
                UiTheme.styleAlert(confirm);
                confirm.showAndWait().ifPresent(bt -> {
                    if (bt == yesBt) {
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
                btnSold.setOnAction(e -> markAdAsSold(finalAdId));
                actionsBox.getChildren().add(btnSold);
            }

            textContainer.getChildren().add(actionsBox);
        } else {
            // ⭐ دکمه علاقه‌مندی برای آگهی دیگران
            HBox actionsBox = new HBox(8);
            actionsBox.setAlignment(Pos.CENTER_LEFT);

            boolean isFav = cachedIsFavoritesView || favoriteAdIds.contains(finalAdId);
            Button btnFav = new Button(isFav
                    ? "\ud83d\udc94 حذف از علاقه‌مندی‌ها"
                    : "\u2764 افزودن به علاقه‌مندی‌ها");
            btnFav.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnFav.setOnMouseClicked(Event::consume);
            btnFav.setOnAction(e -> toggleFavorite(finalAdId, btnFav));

            actionsBox.getChildren().add(btnFav);
            textContainer.getChildren().add(actionsBox);
        }

        card.getChildren().addAll(imageView, textContainer);

        card.setOnMouseClicked(e ->
                openAdDetailsPage(finalAdId, title, description, rawPrice, owner, imageUrl));

        return card;
    }

    // نمایش بزرگ عکس به صورت لایه روی همان صفحه (با کلیک بسته می‌شود)
    private void showEnlargedImage(String imageUrl) {
        javafx.scene.Scene scene = mainBorderPane.getScene();
        if (scene == null) return;

        Parent originalRoot = scene.getRoot();

        ImageView bigView = new ImageView();
        try {
            bigView.setImage(new Image(imageUrl, true));
        } catch (Exception ignored) {
        }
        bigView.setPreserveRatio(true);
        bigView.fitWidthProperty().bind(scene.widthProperty().multiply(0.75));
        bigView.fitHeightProperty().bind(scene.heightProperty().multiply(0.75));

        Label lblHint = new Label("برای بستن، روی صفحه کلیک کنید");
        lblHint.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        VBox content = new VBox(12, bigView, lblHint);
        content.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(content);
        overlay.setStyle("-fx-background-color: rgba(10, 6, 20, 0.88);");
        overlay.setCursor(Cursor.HAND);

        StackPane newRoot = new StackPane(originalRoot, overlay);
        scene.setRoot(newRoot);

        overlay.setOnMouseClicked(e -> {
            e.consume();
            newRoot.getChildren().remove(originalRoot);
            scene.setRoot(originalRoot);
        });
    }

    // ---------------------------------------------------------- ویرایش مشخصات کاربر

    private void openProfileEditPage() {
        setActiveTopBarSection("myDivar");
        setCategorySidebarVisible(false);

        VBox editBox = new VBox(12);
        editBox.setPadding(new Insets(25, 150, 25, 150));
        editBox.setAlignment(Pos.TOP_RIGHT);
        editBox.setStyle("-fx-background-color: #160f29;");

        Label lblHeader = new Label("\u2699\ufe0f ویرایش مشخصات کاربری");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        String inputStyle = "-fx-background-color: #241942; -fx-text-fill: white; -fx-border-color: #3b286b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn';";
        String labelStyle = "-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';";

        Label lblFieldName = new Label("نام و نام خانوادگی:");
        lblFieldName.setStyle(labelStyle);
        TextField txtName = new TextField();
        txtName.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        txtName.setStyle(inputStyle);

        Label lblFieldUsername = new Label("نام کاربری:");
        lblFieldUsername.setStyle(labelStyle);
        TextField txtUsername = new TextField();
        txtUsername.setStyle(inputStyle);

        Label lblFieldPhone = new Label("شماره تماس (شناسه حساب — غیرقابل تغییر):");
        lblFieldPhone.setStyle(labelStyle);
        TextField txtPhone = new TextField();
        txtPhone.setDisable(true);
        txtPhone.setStyle(inputStyle + " -fx-opacity: 0.65;");

        Label lblFieldEmail = new Label("ایمیل:");
        lblFieldEmail.setStyle(labelStyle);
        TextField txtEmail = new TextField();
        txtEmail.setStyle(inputStyle);

        Label lblPassHint = new Label("برای تغییر رمز عبور، دو فیلد زیر را پر کنید؛ در غیر این صورت خالی بگذارید.");
        lblPassHint.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");

        Label lblFieldCurrentPass = new Label("رمز عبور فعلی:");
        lblFieldCurrentPass.setStyle(labelStyle);
        PasswordField txtCurrentPass = new PasswordField();
        txtCurrentPass.setStyle(inputStyle);

        Label lblFieldNewPass = new Label("رمز عبور جدید:");
        lblFieldNewPass.setStyle(labelStyle);
        PasswordField txtNewPass = new PasswordField();
        txtNewPass.setStyle(inputStyle);
        // جلوگیری از فاصله و کاراکترهای ممنوعه در رمز جدید
        txtNewPass.setTextFormatter(new TextFormatter<>(change ->
                change.getText().matches("[A-Za-z0-9@#$%^&*()_+\\-=.!?~]*") ? change : null));

        Button btnSave = new Button("\ud83d\udcbe ذخیره تغییرات");
        btnSave.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #241942; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnSave.setOnAction(e -> saveProfileChanges(
                txtName.getText().trim(),
                txtUsername.getText().trim(),
                txtPhone.getText().trim(),
                txtEmail.getText().trim(),
                txtCurrentPass.getText(),
                txtNewPass.getText()));

        Button btnBack = new Button("بازگشت");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> showHomeScreen());

        HBox buttonBar = new HBox(10, btnSave, btnBack);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        editBox.getChildren().addAll(lblHeader,
                lblFieldName, txtName,
                lblFieldUsername, txtUsername,
                lblFieldPhone, txtPhone,
                lblFieldEmail, txtEmail,
                lblPassHint,
                lblFieldCurrentPass, txtCurrentPass,
                lblFieldNewPass, txtNewPass,
                buttonBar);

        ScrollPane scrollPane = new ScrollPane(editBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);

        // دریافت مشخصات فعلی کاربر از سرور
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/me"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        String body = response.body();
                        txtName.setText(cleanProfileField(extractJsonField(body, "name")));
                        txtUsername.setText(cleanProfileField(extractJsonField(body, "username")));
                        txtPhone.setText(cleanProfileField(extractJsonField(body, "phoneNumber")));
                        txtEmail.setText(cleanProfileField(extractJsonField(body, "email")));
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private String cleanProfileField(String value) {
        return (value == null || "مشخص نشده".equals(value)) ? "" : value;
    }

    private void saveProfileChanges(String name, String username, String phone, String email, String currentPass, String newPass) {
        if (name.isBlank()) {
            showErrorAlert("نام نمی‌تواند خالی باشد.");
            return;
        }
        if (username.isBlank()) {
            showErrorAlert("نام کاربری نمی‌تواند خالی باشد.");
            return;
        }
        if (phone.isBlank()) {
            showErrorAlert("شماره تماس نمی‌تواند خالی باشد.");
            return;
        }
        if (!newPass.isBlank() && currentPass.isBlank()) {
            showErrorAlert("برای تغییر رمز عبور، رمز عبور فعلی را وارد کنید.");
            return;
        }

        StringBuilder body = new StringBuilder("{");
        body.append("\"name\":\"").append(escapeJsonValue(name)).append("\"");
        body.append(",\"username\":\"").append(escapeJsonValue(username)).append("\"");
        body.append(",\"phoneNumber\":\"").append(escapeJsonValue(phone)).append("\"");
        body.append(",\"email\":\"").append(escapeJsonValue(email)).append("\"");
        if (!newPass.isBlank()) {
            body.append(",\"currentPassword\":\"").append(escapeJsonValue(currentPass)).append("\"");
            body.append(",\"newPassword\":\"").append(escapeJsonValue(newPass)).append("\"");
        }
        body.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/users/me"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        // در صورت تغییر نام کاربری، سرور توکن و نام کاربری جدید می‌فرستد
                        String newToken = extractJsonField(response.body(), "token");
                        String newUsername = extractJsonField(response.body(), "username");
                        if (newToken != null && !newToken.isBlank() && !"مشخص نشده".equals(newToken)) {
                            MainApplication.jwtToken = newToken;
                        }
                        if (newUsername != null && !newUsername.isBlank() && !"مشخص نشده".equals(newUsername)) {
                            MainApplication.currentUsername = newUsername;
                        }
                        showSuccessAlert("مشخصات شما با موفقیت به‌روزرسانی شد.");
                        showHomeScreen();
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private String escapeJsonValue(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
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
        VBox editBox = new VBox(20);
        editBox.setPadding(new Insets(30, 150, 30, 150));
        editBox.setAlignment(Pos.TOP_RIGHT);
        editBox.setStyle("-fx-background-color: #160f29;");

        String inputStyle = "-fx-background-color: #241942; -fx-text-fill: white; -fx-border-color: #3b286b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-family: 'Vazirmatn'; -fx-prompt-text-fill: #8b7ca6;";
        String labelStyle = "-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';";
        String comboStyle = "-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white;";

        Label lblHeader = new Label("\u270f\ufe0f ویرایش آگهی");
        lblHeader.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblHint = new Label("پس از ویرایش، آگهی برای بازبینی مجدد به مدیر ارسال می‌شود؛ مگر اینکه فقط عکس اصلی را عوض کرده باشید.");
        lblHint.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");
        lblHint.setWrapText(true);

        Label lblT = new Label("عنوان آگهی"); lblT.setStyle(labelStyle);
        TextField txtTitle = new TextField(title);
        txtTitle.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        txtTitle.setPrefHeight(40); txtTitle.setStyle(inputStyle);
        VBox secTitle = new VBox(6, lblT, txtTitle); secTitle.setAlignment(Pos.TOP_RIGHT);

        Label lblP = new Label("قیمت (تومان)"); lblP.setStyle(labelStyle);
        TextField txtPrice = new TextField(price);
        txtPrice.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        txtPrice.setPrefHeight(40); txtPrice.setStyle(inputStyle);
        VBox secPrice = new VBox(6, lblP, txtPrice); secPrice.setAlignment(Pos.TOP_RIGHT);

        Label lblCat = new Label("دسته‌بندی"); lblCat.setStyle(labelStyle);
        ComboBox<String> cmbCategory = new ComboBox<>();
        if (!serverCategoryTree.isEmpty()) cmbCategory.getItems().addAll(serverCategoryTree.keySet());
        else if (!serverCategories.isEmpty()) cmbCategory.getItems().addAll(serverCategories);
        else cmbCategory.getItems().addAll("کالای دیجیتال", "وسایل نقلیه", "املاک", "لوازم خانگی", "مد و پوشاک", "سرگرمی و فراغت", "خدمات");
        cmbCategory.setMaxWidth(Double.MAX_VALUE); cmbCategory.setPrefHeight(40);
        cmbCategory.setStyle(comboStyle);
        styleComboBox(cmbCategory);

        ComboBox<String> cmbSub = new ComboBox<>();
        cmbSub.setPromptText("انتخاب زیردسته (اختیاری)...");
        cmbSub.setMaxWidth(Double.MAX_VALUE); cmbSub.setPrefHeight(40);
        cmbSub.setStyle(comboStyle);
        styleComboBox(cmbSub); cmbSub.setVisible(false); cmbSub.setManaged(false);

        Runnable refreshSub = () -> {
            String mc = cmbCategory.getValue();
            java.util.List<String> kids = mc == null ? null : serverCategoryTree.get(mc);
            boolean has = kids != null && !kids.isEmpty();
            cmbSub.getItems().setAll(has ? kids : java.util.Collections.<String>emptyList());
            cmbSub.getSelectionModel().clearSelection();
            cmbSub.setVisible(has); cmbSub.setManaged(has);
        };
        cmbCategory.valueProperty().addListener((obs, o, n) -> refreshSub.run());

        if (category != null && !category.isBlank() && !"مشخص نشده".equals(category)) {
            String par = null;
            for (java.util.Map.Entry<String, java.util.List<String>> en : serverCategoryTree.entrySet())
                if (en.getValue().contains(category)) { par = en.getKey(); break; }
            if (par != null) { cmbCategory.setValue(par); cmbSub.setValue(category); }
            else { if (!cmbCategory.getItems().contains(category)) cmbCategory.getItems().add(category); cmbCategory.setValue(category); }
        }

        Label lblCy = new Label("شهر"); lblCy.setStyle(labelStyle);
        ComboBox<String> cmbCity = new ComboBox<>();
        cmbCity.getItems().addAll(serverCities);
        if (city != null && !city.isBlank() && !cmbCity.getItems().contains(city)) cmbCity.getItems().add(city);
        cmbCity.setValue(city);
        cmbCity.setMaxWidth(Double.MAX_VALUE); cmbCity.setPrefHeight(40);
        cmbCity.setStyle(comboStyle);
        styleComboBox(cmbCity);

        VBox catBox = new VBox(6, lblCat, cmbCategory, cmbSub); catBox.setAlignment(Pos.TOP_RIGHT); HBox.setHgrow(catBox, Priority.ALWAYS);
        VBox cityBox = new VBox(6, lblCy, cmbCity); cityBox.setAlignment(Pos.TOP_RIGHT); HBox.setHgrow(cityBox, Priority.ALWAYS);
        HBox catCityRow = new HBox(15, catBox, cityBox); catCityRow.setAlignment(Pos.TOP_RIGHT);

        Label lblD = new Label("توضیحات"); lblD.setStyle(labelStyle);
        TextArea txtDesc = new TextArea(description);
        txtDesc.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        txtDesc.setPrefRowCount(4); txtDesc.setWrapText(true);
        txtDesc.setStyle(inputStyle + " -fx-control-inner-background: #241942;");
        VBox secDesc = new VBox(6, lblD, txtDesc); secDesc.setAlignment(Pos.TOP_RIGHT);

        Label lblImgTitle = new Label("تصاویر آگهی — عکس اصلی کادر طلایی دارد؛ برای تغییر آن دکمه \u2b50 عکس دیگری را بزنید");
        lblImgTitle.setStyle(labelStyle); lblImgTitle.setWrapText(true);

        List<String> keptImgs = new ArrayList<>();
        if (currentImageUrl != null && !currentImageUrl.isBlank() && !"مشخص نشده".equals(currentImageUrl))
            for (String p : currentImageUrl.split(",")) { String t = p.trim(); if (!t.isEmpty()) keptImgs.add(t); }
        final List<String> origImgs = new ArrayList<>(keptImgs);
        List<File> newImgFiles = new ArrayList<>();
        final int[] mainIdx = {0};

        FlowPane thumbsPane = new FlowPane(10, 10);
        thumbsPane.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        thumbsPane.setPadding(new Insets(5));

        final Runnable[] renderThumbs = new Runnable[1];
        renderThumbs[0] = () -> {
            thumbsPane.getChildren().clear();
            int tot = keptImgs.size() + newImgFiles.size();
            if (mainIdx[0] >= tot && tot > 0) mainIdx[0] = 0;
            for (int i = 0; i < keptImgs.size(); i++) {
                final int fi = i; final String src = keptImgs.get(fi);
                thumbsPane.getChildren().add(buildImageThumb(
                    new Image(BASE_URL + src, 140, 100, false, true, true), "فعلی", fi == mainIdx[0],
                    () -> { keptImgs.remove(src); if (mainIdx[0] >= keptImgs.size() + newImgFiles.size()) mainIdx[0] = 0; renderThumbs[0].run(); },
                    () -> { mainIdx[0] = fi; renderThumbs[0].run(); }));
            }
            for (int i = 0; i < newImgFiles.size(); i++) {
                final int fi = keptImgs.size() + i; final File ff = newImgFiles.get(i);
                thumbsPane.getChildren().add(buildImageThumb(
                    new Image(ff.toURI().toString(), 140, 100, false, true, true), "جدید", fi == mainIdx[0],
                    () -> { newImgFiles.remove(ff); if (mainIdx[0] >= keptImgs.size() + newImgFiles.size()) mainIdx[0] = 0; renderThumbs[0].run(); },
                    () -> { mainIdx[0] = fi; renderThumbs[0].run(); }));
            }
            if (thumbsPane.getChildren().isEmpty()) { Label le = new Label("این آگهی فعلاً هیچ عکسی ندارد."); le.setStyle(labelStyle); thumbsPane.getChildren().add(le); }
        };
        renderThumbs[0].run();

        Button btnAdd = new Button("\ud83d\uddbc\ufe0f افزودن عکس (چندانتخابی با Ctrl)");
        btnAdd.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");
        btnAdd.setPrefHeight(42); btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setOnAction(ev -> {
            FileChooser fc = new FileChooser(); fc.setTitle("افزودن عکس به آگهی");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("تصاویر", "*.png", "*.jpg", "*.jpeg"));
            List<File> picked = fc.showOpenMultipleDialog(mainBorderPane.getScene().getWindow());
            if (picked != null && !picked.isEmpty()) { newImgFiles.addAll(picked); renderThumbs[0].run(); }
        });
        VBox secImgs = new VBox(8, lblImgTitle, btnAdd, thumbsPane); secImgs.setAlignment(Pos.TOP_RIGHT);

        Button btnSave = new Button("\ud83d\udcbe ذخیره همه تغییرات");
        btnSave.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px;");
        btnSave.setPrefHeight(45); HBox.setHgrow(btnSave, Priority.ALWAYS); btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(ev -> {
            try { Double.parseDouble(txtPrice.getText().trim()); }
            catch (NumberFormatException nfe) { showErrorAlert("لطفاً برای قیمت فقط عدد انگلیسی وارد کنید."); return; }
            List<String> ordK = new ArrayList<>(keptImgs); List<File> ordN = new ArrayList<>(newImgFiles);
            int mi = mainIdx[0];
            if (mi > 0 && mi < ordK.size()) ordK.add(0, ordK.remove(mi));
            else if (mi >= ordK.size()) { int ni = mi - ordK.size(); if (ni > 0 && ni < ordN.size()) ordN.add(0, ordN.remove(ni)); }
            String cityVal = cmbCity.getValue() == null ? "" : cmbCity.getValue();
            String catMain = cmbCategory.getValue() == null ? "" : cmbCategory.getValue();
            String catVal = (cmbSub.isVisible() && cmbSub.getValue() != null && !cmbSub.getValue().isBlank()) ? cmbSub.getValue() : catMain;
            if (!ordN.isEmpty()) {
                final List<String> fk = ordK;
                uploadEditImages(ordN).thenAccept(joined -> Platform.runLater(() -> {
                    List<String> all = new ArrayList<>(fk);
                    for (String p : joined.split(",")) { String t = p.trim(); if (!t.isEmpty()) all.add(t); }
                    sendEditRequest(adId, txtTitle.getText(), txtDesc.getText(), txtPrice.getText(), cityVal, catVal, String.join(",", all), true);
                })).exceptionally(ex -> { Platform.runLater(() -> showErrorAlert("آپلود عکس‌های جدید خطا: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()))); return null; });
            } else {
                boolean textChanged =
                        !txtTitle.getText().equals(title == null ? "" : title)
                        || !txtDesc.getText().equals(description == null ? "" : description)
                        || !txtPrice.getText().trim().equals(price == null ? "" : price.trim())
                        || !cityVal.equals(city == null ? "" : city)
                        || !catVal.equals(category == null ? "" : category);
                boolean sameImages = ordK.size() == origImgs.size()
                        && new java.util.HashSet<>(ordK).equals(new java.util.HashSet<>(origImgs));
                if (!textChanged && sameImages) {
                    // فقط عکس اصلی عوض شده — بدون نیاز به تایید مجدد مدیر
                    sendImageOnlyRequest(adId, String.join(",", ordK));
                } else {
                    sendEditRequest(adId, txtTitle.getText(), txtDesc.getText(), txtPrice.getText(), cityVal, catVal, String.join(",", ordK), true);
                }
            }
        });

        Button btnBack = new Button("\u274c انصراف");
        btnBack.setStyle("-fx-background-color: transparent; -fx-border-color: #ff5555; -fx-border-radius: 6; -fx-text-fill: #ff5555; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px;");
        btnBack.setPrefHeight(45); HBox.setHgrow(btnBack, Priority.ALWAYS); btnBack.setMaxWidth(Double.MAX_VALUE);
        btnBack.setOnAction(e -> onLoadMyAdsClick());

        HBox actionRow = new HBox(15, btnBack, btnSave);
        actionRow.setAlignment(Pos.CENTER);
        VBox.setMargin(actionRow, new Insets(15, 0, 0, 0));

        editBox.getChildren().addAll(lblHeader, lblHint, secTitle, secPrice, catCityRow, secDesc, secImgs, actionRow);

        ScrollPane scrollPane = new ScrollPane(editBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
    }

    /** \ud83d\uddbc\ufe0f پیش‌نمایش کوچک عکس؛ عکس اصلی کادر طلایی دارد و بقیه دکمه \u2b50 دارند. */
    private VBox buildImageThumb(Image image, String tag, boolean isMain, Runnable onRemove, Runnable onSetMain) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(140); iv.setFitHeight(100); iv.setPreserveRatio(false);

        Label lblTag = new Label(isMain ? "\u2b50 عکس اصلی" : tag);
        lblTag.setStyle(isMain
            ? "-fx-text-fill: #ffc83b; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn'; -fx-font-weight: bold;"
            : "-fx-text-fill: #b9a6df; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");

        Button btnR = new Button("\ud83d\uddd1\ufe0f حذف");
        btnR.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn';");
        btnR.setOnAction(e -> onRemove.run());

        VBox box = new VBox(5, iv, lblTag, btnR);
        if (!isMain) {
            Button bm = new Button("\u2b50 عکس اصلی");
            bm.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 10px; -fx-font-family: 'Vazirmatn';");
            bm.setOnAction(e -> onSetMain.run());
            box.getChildren().add(bm);
        }
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #241942; -fx-border-color: " + (isMain ? "#ffc83b" : "#3b286b") + "; -fx-border-width: " + (isMain ? "2" : "1") + "; -fx-border-radius: 10; -fx-background-radius: 10;");
        return box;
    }

    /** ذخیره فقط عکس آگهی — بدون بازگشت به صف تایید مدیر. */
    private void sendImageOnlyRequest(long adId, String imageUrl) {
        String safe = imageUrl == null ? "" : escapeJson(imageUrl);
        String body = "{\"image_url\":\"" + safe + "\",\"imageUrl\":\"" + safe + "\",\"onlyImageUpdate\":true}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/advertisements/" + adId))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    checkAndRefreshToken(response);
                    if (response.statusCode() == 200) {
                        showSuccessAlert("عکس اصلی آگهی بروز شد؛ آگهی همچنان فعال می‌ماند و نیازی به تایید مجدد مدیر ندارد.");
                        onLoadMyAdsClick();
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
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


    // ——— نمایشگر تصویر با قابلیت زوم و جابجایی ———
    private void showImageZoom(String imageUrl) {
        javafx.stage.Stage zoomStage = new javafx.stage.Stage();
        zoomStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        zoomStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        zoomStage.setTitle("نمایش تصویر");

        ImageView iv = new ImageView(new Image(imageUrl, true));
        iv.setPreserveRatio(true);
        iv.setFitWidth(800);
        iv.setFitHeight(600);
        iv.setSmooth(true);

        javafx.scene.layout.StackPane imgHolder = new javafx.scene.layout.StackPane(iv);
        imgHolder.setStyle("-fx-background-color: transparent;");
        // كليپ: عكس از محدوده StackPane بيرون نمي‌زند (روي نوار دكمه‌ها نمي‌آيد)
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        imgHolder.layoutBoundsProperty().addListener((obs, ov, nv) -> {
            clip.setWidth(nv.getWidth());
            clip.setHeight(nv.getHeight());
        });
        imgHolder.setClip(clip);

        // دکمه‌های زوم + / -
        Button btnZoomIn  = new Button("+");
        Button btnZoomOut = new Button("-");
        Button btnClose   = new Button("✕ بستن");
        String zBtnStyle = "-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; "
            + "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; "
            + "-fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-min-width: 44; -fx-min-height: 36;";
        btnZoomIn.setStyle(zBtnStyle);
        btnZoomOut.setStyle(zBtnStyle);
        btnClose.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; "
            + "-fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 8; "
            + "-fx-cursor: hand; -fx-font-family: 'Vazirmatn';");

        final double[] scale = {1.0};
        Runnable applyScale = () -> {
            scale[0] = Math.max(0.3, Math.min(scale[0], 5.0));
            iv.setScaleX(scale[0]);
            iv.setScaleY(scale[0]);
        };
        btnZoomIn.setOnAction(e  -> { scale[0] += 0.2; applyScale.run(); });
        btnZoomOut.setOnAction(e -> { scale[0] -= 0.2; applyScale.run(); });
        btnClose.setOnAction(e   -> zoomStage.close());

        HBox toolbar = new HBox(10, btnZoomIn, btnZoomOut, btnClose);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(8));
        toolbar.setStyle("-fx-background-color: #160f29;");

        // زوم با اسکرول ماوس
        iv.setOnScroll(ev -> {
            scale[0] += ev.getDeltaY() > 0 ? 0.1 : -0.1;
            applyScale.run();
        });

        // دراگ برای جابجایی
        final double[] dragStart = {0, 0};
        iv.setOnMousePressed(ev -> { dragStart[0] = ev.getSceneX() - iv.getTranslateX();
                                     dragStart[1] = ev.getSceneY() - iv.getTranslateY(); });
        iv.setOnMouseDragged(ev -> { iv.setTranslateX(ev.getSceneX() - dragStart[0]);
                                     iv.setTranslateY(ev.getSceneY() - dragStart[1]); });

        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        root.setTop(toolbar);
        root.setCenter(imgHolder);
        root.setStyle("-fx-background-color: #0d0820;");

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 860, 660);
        // ESC برای بستن
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == javafx.scene.input.KeyCode.ESCAPE) zoomStage.close();
            else if (ev.getCode() == javafx.scene.input.KeyCode.PLUS
                  || ev.getCode() == javafx.scene.input.KeyCode.EQUALS) { scale[0] += 0.2; applyScale.run(); }
            else if (ev.getCode() == javafx.scene.input.KeyCode.MINUS) { scale[0] -= 0.2; applyScale.run(); }
        });
        zoomStage.setScene(scene);
        zoomStage.show();
    }

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

            imageViewGallery.setCursor(javafx.scene.Cursor.HAND);
            imageViewGallery.setOnMouseClicked(ev -> {
                String url = BASE_URL + imageSources.get(currentIndex[0]);
                showImageZoom(url);
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
            btnRate.setOnAction(e -> openRatingPage(adId, title, description, rawPrice, owner, imageUrl));

            boolean isFavDetail = favoriteAdIds.contains(adId);
            Button btnFavDetail = new Button(isFavDetail
                    ? "\ud83d\udc94 حذف از علاقه‌مندی‌ها"
                    : "\u2764 افزودن به علاقه‌مندی‌ها");
            btnFavDetail.setStyle("-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnFavDetail.setOnAction(e -> toggleFavorite(adId, btnFavDetail));

            actionRow.getChildren().addAll(btnStartChat, btnRate, btnFavDetail);
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
                            target.setText("\u2b50 امتیاز فروشنده: " + average + " (از " + count + " رای)");
                        }
                    } else {
                        target.setText("\u2b50 امتیاز فروشنده: نامشخص");
                    }
                }));
    }

    private void openRatingPage(long adId, String title, String description,
                                 String rawPrice, String owner, String imageUrl) {
        VBox ratingBox = new VBox(20);
        ratingBox.setPadding(new Insets(30));
        ratingBox.setAlignment(Pos.TOP_CENTER);
        ratingBox.setStyle("-fx-background-color: #160f29;");

        Button btnBack = new Button("\u2b05 بازگشت به آگهی");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> openAdDetailsPage(adId, title, description, rawPrice, owner, imageUrl));

        HBox backRow = new HBox(btnBack);
        backRow.setAlignment(Pos.CENTER_RIGHT);

        Label lblHeader = new Label("\u2b50 ثبت امتیاز به فروشنده");
        lblHeader.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Label lblSeller = new Label("فروشنده: " + owner);
        lblSeller.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");

        Label lblAd = new Label("آگهی: " + title);
        lblAd.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        final int[] selectedScore = {5};
        Button[] starButtons = new Button[5];

        Label lblSelected = new Label();
        lblSelected.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");

        Runnable refreshStars = () -> {
            for (int i = 0; i < 5; i++) {
                boolean active = i < selectedScore[0];
                starButtons[i].setStyle("-fx-background-color: " + (active ? "#ffc83b" : "#241942")
                        + "; -fx-text-fill: " + (active ? "#160f29" : "#8575a3")
                        + "; -fx-font-size: 22px; -fx-background-radius: 10; -fx-cursor: hand;"
                        + " -fx-border-color: " + (active ? "#d8a014" : "#3b286b")
                        + "; -fx-border-radius: 10;");
            }
            lblSelected.setText("امتیاز انتخاب شده: " + selectedScore[0] + " از 5");
        };

        for (int i = 0; i < 5; i++) {
            final int score = i + 1;
            Button star = new Button("\u2605");
            star.setOnAction(e -> {
                selectedScore[0] = score;
                refreshStars.run();
            });
            starButtons[i] = star;
        }

        HBox starsRow = new HBox(10);
        starsRow.setAlignment(Pos.CENTER);
        for (int i = 4; i >= 0; i--) {
            starsRow.getChildren().add(starButtons[i]);
        }
        refreshStars.run();

        Button btnSubmit = new Button("ثبت امتیاز");
        btnSubmit.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnSubmit.setOnAction(e -> {
            String body = "{\"adId\":" + adId + ",\"score\":" + selectedScore[0] + "}";

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
                            openAdDetailsPage(adId, title, description, rawPrice, owner, imageUrl);
                        } else if (response.statusCode() == 401) {
                            handleUnauthorized(response.statusCode());
                        } else {
                            showErrorAlert(extractJsonField(response.body(), "message"));
                        }
                    }));
        });

        VBox card = new VBox(18, lblHeader, lblSeller, lblAd, starsRow, lblSelected, btnSubmit);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(460);
        card.setStyle("-fx-background-color: #241942; -fx-background-radius: 14; -fx-border-color: #3b286b; -fx-border-radius: 14;");

        ratingBox.getChildren().addAll(backRow, card);

        ScrollPane scrollPane = new ScrollPane(ratingBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        mainBorderPane.setCenter(scrollPane);
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
                        long convId;
                        try {
                            convId = Long.parseLong(convIdStr);
                        } catch (NumberFormatException e) {
                            convId = -1L;
                        }
                        // اگر گفتگویی وجود نداشته باشد، پنجره چت در حالت «در انتظار اولین پیام» باز می‌شود
                        openChatPane(convId, adId, sellerUsername, adTitle);
                    } else if (response.statusCode() == 401) {
                        handleUnauthorized(response.statusCode());
                    } else {
                        showErrorAlert(extractJsonField(response.body(), "message"));
                    }
                }));
    }

    private void openChatPane(long conversationId, long adId, String otherUser, String adTitle) {
        final long[] convIdHolder = { conversationId };
        setCategorySidebarVisible(false);
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
        btnRefresh.setOnAction(e -> {
            if (convIdHolder[0] > 0) {
                loadChatMessages(convIdHolder[0], messagesBox);
            }
        });

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

            boolean firstMessage = convIdHolder[0] <= 0;
            String body = firstMessage
                    ? "{\"adId\":" + adId + ",\"content\":\"" + escapeJson(text) + "\"}"
                    : "{\"content\":\"" + escapeJson(text) + "\"}";
            String url = firstMessage
                    ? BASE_URL + "/api/chat/send-first"
                    : BASE_URL + "/api/chat/conversations/" + convIdHolder[0] + "/messages";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        checkAndRefreshToken(response);
                        if (response.statusCode() == 200) {
                            if (firstMessage) {
                                String newConvIdStr = extractJsonField(response.body(), "conversationId");
                                try {
                                    convIdHolder[0] = Long.parseLong(newConvIdStr);
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            input.clear();
                            if (convIdHolder[0] > 0) {
                                loadChatMessages(convIdHolder[0], messagesBox);
                            }
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
        if (convIdHolder[0] > 0) {
            loadChatMessages(convIdHolder[0], messagesBox);
        } else {
            Label lblEmpty = new Label("هنوز گفتگویی شکل نگرفته است. با ارسال اولین پیام، گفتگو ساخته می‌شود.");
            lblEmpty.setWrapText(true);
            lblEmpty.setStyle("-fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn';");
            HBox emptyRow = new HBox(lblEmpty);
            emptyRow.setAlignment(Pos.CENTER);
            messagesBox.getChildren().add(emptyRow);
        }
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

    // دریافت لیست شهرها از سرور تا تغییرات ادمین (افزودن/ویرایش/حذف) اعمال شود
    private void loadCitiesFromServer() {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(BASE_URL + "/api/cities"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        java.net.http.HttpClient.newHttpClient()
                .sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
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
                    javafx.application.Platform.runLater(() -> {
                        serverCities.clear();
                        serverCities.addAll(names);
                        if (cityFilterCombo != null) {
                            java.util.List<String> desired = new java.util.ArrayList<>();
                            desired.add("همه شهرها");
                            desired.addAll(names);
                            if (!desired.equals(new java.util.ArrayList<>(cityFilterCombo.getItems()))) {
                                String current = cityFilterCombo.getValue();
                                cityFilterCombo.getItems().setAll(desired);
                                if (current != null && desired.contains(current)) {
                                    cityFilterCombo.setValue(current);
                                }
                            }
                        }
                    });
                });
    }

    private void openAdminPanel() {
        setActiveTopBarSection("myDivar");
        setCategorySidebarVisible(false);
        mainBorderPane.setCenter(new AdminPanelController(() -> {
            loadCitiesFromServer(); // اعمال تغییرات شهرها پس از خروج از پنل مدیریت
            showHomeScreen();
        }).createView());
    }

    // ---------------------------------------------------------- ثبت آگهی و خروج

    @FXML
    protected void onRegisterAdScreenClick() {
        setActiveTopBarSection("register");
        setCategorySidebarVisible(false);
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
