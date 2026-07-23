package com.example.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

/**
 * 🛡️ پنل مدیریت (فقط برای کاربر با نقش ADMIN نمایش داده می‌شود)
 * شامل چهار تب: مدیریت آگهی‌ها، مدیریت کاربران، دسته‌بندی‌ها و داشبورد آماری
 */
public class AdminPanelController {

    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Runnable onBack;

    private VBox adsBox;
    private VBox usersBox;
    private VBox categoriesBox;
    private VBox citiesBox;
    private VBox statsBox;
    private ComboBox<String> statusCombo;

    /**
     * سازنده کلاس AdminPanelController؛ نمونه جدید با مقادیر داده‌شده ایجاد می‌کند.
     *
     * @param onBack پارامتر onBack
     */
    public AdminPanelController(Runnable onBack) {
        this.onBack = onBack;
    }

    // ---------------------------------------------------------- UI

    /**
     * «view» جدید ایجاد می‌کند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    public BorderPane createView() {
        BorderPane root = new BorderPane();
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-background-color: #160f29;");

        // هدر بالا
        Label title = new Label("\ud83d\udee1\ufe0f پنل مدیریت سیستم");
        title.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        Button btnBack = new Button("⬅ بازگشت به صفحه اصلی");
        btnBack.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnBack.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(15, title, spacer, btnBack);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));

        BorderPane contentHolder = new BorderPane();

        Button adsTabBtn = buildTabButton("\ud83d\udd52 مدیریت آگهی‌ها", buildAdsTab(), contentHolder);
        Button usersTabBtn = buildTabButton("\ud83d\udc65 کاربران", buildUsersTab(), contentHolder);
        Button categoriesTabBtn = buildTabButton("\ud83d\uddc2\ufe0f دسته‌بندی‌ها", buildCategoriesTab(), contentHolder);
        Button citiesTabBtn = buildTabButton("\ud83c\udfd9\ufe0f شهرها", buildCitiesTab(), contentHolder);
        Button statsTabBtn = buildTabButton("\ud83d\udcca آمار", buildStatsTab(), contentHolder);

        HBox tabBar = new HBox(10, adsTabBtn, usersTabBtn, categoriesTabBtn, citiesTabBtn, statsTabBtn);
        tabBar.setPadding(new Insets(0, 20, 12, 20));

        VBox centerBox = new VBox(tabBar, contentHolder);
        VBox.setVgrow(contentHolder, Priority.ALWAYS);

        root.setTop(header);
        root.setCenter(centerBox);

        adsTabBtn.fire();

        // بارگذاری اولیه داده‌ها
        loadAds("PENDING");
        loadUsers();
        loadCategories();
        loadCities();
        loadStats();

        return root;
    }

    private final java.util.List<Button> tabButtons = new java.util.ArrayList<>();

    private static final String TAB_NORMAL_STYLE = "-fx-background-color: #241942; -fx-text-fill: #b9a6df; -fx-background-radius: 8; -fx-border-color: #3b286b; -fx-border-radius: 8; -fx-border-width: 1; -fx-padding: 6 16 6 16; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;";
    private static final String TAB_ACTIVE_STYLE = "-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #ffc83b; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 6 16 6 16; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;";

    /**
     * کامپوننت/ساختار «tab button» را می‌سازد و برمی‌گرداند.
     *
     * @param text متن
     * @param content پارامتر content
     * @param holder پارامتر holder
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Button buildTabButton(String text, Node content, BorderPane holder) {
        Button b = new Button(text);
        b.setStyle(TAB_NORMAL_STYLE);
        b.setOnAction(e -> {
            holder.setCenter(content);
            for (Button other : tabButtons) {
                other.setStyle(TAB_NORMAL_STYLE);
            }
            b.setStyle(TAB_ACTIVE_STYLE);
        });
        tabButtons.add(b);
        return b;
    }

    /**
     * «scroll» را در قالب مناسب قرار می‌دهد.
     *
     * @param content پارامتر content
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private ScrollPane wrapScroll(VBox content) {
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: #160f29;");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");
        return scroll;
    }

    /**
     * «card» جدید ایجاد می‌کند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private VBox createCard() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #241942; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-radius: 10;");
        return card;
    }

    /**
     * متد «infoLabel»؛ بخشی از عملکرد کلاس AdminPanelController را پیاده‌سازی می‌کند.
     *
     * @param text متن
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Label infoLabel(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");
        return lbl;
    }

    /**
     * متد «mutedLabel»؛ بخشی از عملکرد کلاس AdminPanelController را پیاده‌سازی می‌کند.
     *
     * @param text متن
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Label mutedLabel(String text) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';");
        return lbl;
    }

    // ---------------------------------------------------------- تب آگهی‌ها

    /**
     * کامپوننت/ساختار «ads tab» را می‌سازد و برمی‌گرداند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildAdsTab() {
        adsBox = new VBox(10);

        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("PENDING", "ACTIVE", "REJECTED", "SOLD", "ALL");
        statusCombo.setValue("PENDING");
        statusCombo.setPrefHeight(34);
        statusCombo.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        HelloController.styleComboBox(statusCombo);
        statusCombo.setOnAction(e -> loadAds(statusCombo.getValue()));

        Button btnRefresh = new Button("\ud83d\udd04 بروزرسانی");
        btnRefresh.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnRefresh.setOnAction(e -> loadAds(statusCombo.getValue()));

        HBox controls = new HBox(10, mutedLabel("فیلتر وضعیت:"), statusCombo, btnRefresh);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(10, 15, 0, 15));

        VBox container = new VBox(10, controls, wrapScroll(adsBox));
        VBox.setVgrow(container.getChildren().get(1), Priority.ALWAYS);
        container.setStyle("-fx-background-color: #160f29;");
        return container;
    }

    /**
     * داده‌های مربوط به «ads» را بارگذاری و نمایش می‌دهد.
     *
     * @param status پارامتر status
     */
    private void loadAds(String status) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/advertisements?status=" + status))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    adsBox.getChildren().clear();
                    if (response.statusCode() != 200) {
                        adsBox.getChildren().add(mutedLabel("خطا در دریافت آگهی‌ها (کد " + response.statusCode() + ")"));
                        return;
                    }
                    try {
                        JsonNode data = mapper.readTree(response.body()).path("data");
                        if (!data.isArray() || data.isEmpty()) {
                            adsBox.getChildren().add(mutedLabel("آگهی‌ای با این وضعیت وجود ندارد."));
                            return;
                        }
                        for (JsonNode ad : data) {
                            adsBox.getChildren().add(buildAdCard(ad));
                        }
                    } catch (Exception ex) {
                        adsBox.getChildren().add(mutedLabel("خطا در پردازش اطلاعات."));
                    }
                }));
    }

    /**
     * ساخت کارت گرافیکی یک آگهی شامل تصویر، عنوان، قیمت و دکمه‌های عملیات.
     *
     * @param ad آگهی
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildAdCard(JsonNode ad) {
        long id = ad.path("id").asLong();
        String status = ad.path("status").asText("");

        VBox card = createCard();
        card.getChildren().add(infoLabel("\ud83c\udff7\ufe0f " + ad.path("title").asText("بدون عنوان")));
        card.getChildren().add(mutedLabel("فروشنده: " + ad.path("ownerUsername").asText("-")
                + "  |  قیمت: " + ad.path("price").asText("-")
                + "  |  شهر: " + ad.path("city").asText("-")
                + "  |  دسته: " + ad.path("category").asText("-")
                + "  |  وضعیت: " + status));

        String description = ad.path("description").asText("");
        if (!description.isBlank()) {
            card.getChildren().add(mutedLabel(description));
        }
        String adminNote = ad.path("adminNote").asText("");
        if (!adminNote.isBlank() && !"null".equals(adminNote)) {
            card.getChildren().add(mutedLabel("\ud83d\udcdd یادداشت مدیر: " + adminNote));
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnView = new Button("👁 مشاهده کامل");
        btnView.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnView.setOnAction(e -> showAdDetail(ad));
        actions.getChildren().add(btnView);

        if (!"ACTIVE".equalsIgnoreCase(status)) {
            Button btnApprove = new Button("✅ تایید");
            btnApprove.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnApprove.setOnAction(e -> postAdminAction("/api/admin/advertisements/" + id + "/approve", null,
                    () -> loadAds(statusCombo.getValue())));
            actions.getChildren().add(btnApprove);
        }

        if (!"REJECTED".equalsIgnoreCase(status)) {
            Button btnReject = new Button("❌ رد");
            btnReject.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
            btnReject.setOnAction(e -> showRejectReasonDialog(note -> {
                String body = "{\"note\":\"" + note.replace("\"", "\\\"") + "\"}";
                postAdminAction("/api/admin/advertisements/" + id + "/reject", body,
                        () -> loadAds(statusCombo.getValue()));
            }));
            actions.getChildren().add(btnReject);
        }

        Button btnDelete = new Button("\ud83d\uddd1\ufe0f حذف");
        btnDelete.setStyle("-fx-background-color: #4a148c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnDelete.setOnAction(e -> {
            ButtonType yesBt = new ButtonType("بله، حذف شود", javafx.scene.control.ButtonBar.ButtonData.YES);
            ButtonType noBt = new ButtonType("انصراف", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "این آگهی برای همیشه حذف شود؟", yesBt, noBt);
            confirm.setHeaderText(null);
            confirm.setTitle("تایید حذف آگهی");
            UiTheme.styleAlert(confirm);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == yesBt) {
                    deleteAdminAction("/api/admin/advertisements/" + id, () -> loadAds(statusCombo.getValue()));
                }
            });
        });
        actions.getChildren().add(btnDelete);

        card.getChildren().add(actions);
        return card;
    }

    // ---------------------------------------------------------- نمایش کامل آگهی برای مدیر

    /** 👁 نمایش کامل آگهی (عکس‌ها، توضیحات و مشخصات) در پنجره جداگانه */
    /**
     * دیالوگ داخلی تکمیلی برای دریافت دلیل رد آگهی.
     * عمداً از TextInputDialog/DialogPane استفاده نمی‌کنیم چون محاسبهٔ عرض داخلی ButtonBar
     * توسط جاوافکس با ترتیب راست‌به-چپ (RTL) ترکیب می‌شود و دکمهٔ «انصراف» را بریده/کوتاه
     * نمایش می‌دهد. این متد یک پنجرهٔ کاملاً دستی با Stage + VBox می‌سازد که دکمه‌ها
     * در یک HBox معمولی قرار دارند (بدون هیچ محاسبهٔ عرض یکنواختی از سمت JavaFX)،
     * برای همین همیشه به اندازهٔ واقعی متن دکمه رندر می‌شوند و هرگز بریده نمی‌شوند.
     *
     * @param onConfirm وقتی کاربر دکمهٔ «تأیید» را بزند، با متن یادداشت (ممکن است خالی باشد) فراخوانی می‌شود
     */
    private void showRejectReasonDialog(Consumer<String> onConfirm) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("رد آگهی");
        dlg.setResizable(false);

        Label header = new Label("دلیل رد آگهی را وارد کنید (اختیاری):");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");
        header.setWrapText(true);
        header.setMaxWidth(360);

        TextField field = new TextField();
        field.setPromptText("یادداشت:");
        field.setPrefWidth(360);
        field.setStyle("-fx-background-color: #160f29; -fx-text-fill: white;"
                + "-fx-prompt-text-fill: #8b7ca6;"
                + "-fx-border-color: #3b286b; -fx-border-radius: 8; -fx-background-radius: 8;"
                + "-fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-padding: 8;");

        Button btnOk = new Button("تأیید");
        btnOk.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-background-radius: 8;"
                + "-fx-cursor: hand; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;"
                + "-fx-padding: 8 26 8 26;");
        btnOk.setDefaultButton(true);

        Button btnCancel = new Button("انصراف");
        btnCancel.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8;"
                + "-fx-cursor: hand; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;"
                + "-fx-padding: 8 26 8 26;");
        btnCancel.setCancelButton(true);

        HBox buttons = new HBox(10, btnOk, btnCancel);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(16, header, field, buttons);
        root.setPadding(new Insets(22, 28, 22, 28));
        root.setAlignment(Pos.TOP_RIGHT);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-background-color: #241942; -fx-border-color: #ffc83b; -fx-border-width: 1.5px;");

        btnOk.setOnAction(ev -> {
            String note = field.getText() == null ? "" : field.getText();
            dlg.close();
            onConfirm.accept(note);
        });
        btnCancel.setOnAction(ev -> dlg.close());

        Scene scene = new Scene(root);
        dlg.setScene(scene);
        dlg.sizeToScene();
        dlg.showAndWait();
    }

    /**
     * «ad detail» را به کاربر نمایش می‌دهد.
     *
     * @param ad آگهی
     */
    private void showAdDetail(JsonNode ad) {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("مشاهده آگهی: " + ad.path("title").asText(""));

        VBox root = new VBox(15);
        root.setPadding(new Insets(25, 35, 25, 35));
        root.setAlignment(Pos.TOP_RIGHT);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-background-color: #160f29;");

        String keyStyle = "-fx-text-fill: #b9a6df; -fx-font-size: 12px; -fx-font-family: 'Vazirmatn';";
        String valStyle = "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn'; -fx-font-weight: bold;";

        Label hdr = new Label("🏷️ " + ad.path("title").asText("بدون عنوان"));
        hdr.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        String status = ad.path("status").asText("-");
        String stColor = "ACTIVE".equalsIgnoreCase(status) ? "#2e7d32"
                : "REJECTED".equalsIgnoreCase(status) ? "#b71c1c"
                : "PENDING".equalsIgnoreCase(status) ? "#e65100" : "#3b286b";
        Label lblStatus = new Label("وضعیت: " + status);
        lblStatus.setStyle("-fx-background-color: " + stColor + "; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 10 4 10; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");

        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(8);
        String[][] rows = {
            {"فروشنده", ad.path("ownerUsername").asText("-")},
            {"قیمت", ad.path("price").asText("-") + " تومان"},
            {"شهر", ad.path("city").asText("-")},
            {"دسته‌بندی", ad.path("category").asText("-")},
        };
        for (int r = 0; r < rows.length; r++) {
            Label k = new Label(rows[r][0] + ":"); k.setStyle(keyStyle);
            Label v = new Label(rows[r][1]); v.setStyle(valStyle);
            grid.add(k, 0, r); grid.add(v, 1, r);
        }

        Label lblDescHdr = new Label("توضیحات:"); lblDescHdr.setStyle(keyStyle);
        String desc = ad.path("description").asText("");
        Label lblDesc = new Label(desc.isBlank() ? "—" : desc);
        lblDesc.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");
        lblDesc.setWrapText(true); lblDesc.setMaxWidth(560);

        VBox noteBox = new VBox(4);
        String note = ad.path("adminNote").asText("");
        if (!note.isBlank() && !"null".equals(note)) {
            Label nh = new Label("📝 یادداشت مدیر:"); nh.setStyle(keyStyle);
            Label nv = new Label(note);
            nv.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");
            nv.setWrapText(true); nv.setMaxWidth(560);
            noteBox.getChildren().addAll(nh, nv);
        }

        Label imgHdr = new Label("🖼️ تصاویر آگهی:"); imgHdr.setStyle(keyStyle);
        FlowPane gallery = new FlowPane(10, 10);
        String rawImg = ad.path("imageUrl").asText("");
        if (rawImg.isBlank() || "null".equals(rawImg)) rawImg = ad.path("image_url").asText("");
        if (!rawImg.isBlank() && !"null".equals(rawImg)) {
            boolean first = true;
            for (String p : rawImg.split(",")) {
                String src = p.trim();
                if (src.isEmpty()) continue;
                String full = src.startsWith("http") ? src : BASE_URL + src;
                try {
                    ImageView iv = new ImageView(new Image(full, 180, 135, false, true, true));
                    iv.setFitWidth(180); iv.setFitHeight(135); iv.setPreserveRatio(false);
                    VBox ib = new VBox(4, iv);
                    if (first) {
                        Label ml = new Label("⭐ عکس اصلی");
                        ml.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 11px; -fx-font-family: 'Vazirmatn'; -fx-font-weight: bold;");
                        ib.getChildren().add(ml);
                        first = false;
                    }
                    ib.setAlignment(Pos.CENTER);
                    ib.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6;");
                    gallery.getChildren().add(ib);
                } catch (Exception ignored) {}
            }
        }
        if (gallery.getChildren().isEmpty()) {
            Label nl = new Label("این آگهی عکسی ندارد.");
            nl.setStyle("-fx-text-fill: #8b7ca6; -fx-font-family: 'Vazirmatn';");
            gallery.getChildren().add(nl);
        }

        Button btnClose = new Button("✖ بستن");
        btnClose.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn'; -fx-font-size: 14px;");
        btnClose.setPrefHeight(40); btnClose.setMinWidth(130);
        btnClose.setOnAction(e -> dlg.close());
        HBox closeRow = new HBox(btnClose);
        closeRow.setAlignment(Pos.CENTER);
        VBox.setMargin(closeRow, new Insets(10, 0, 0, 0));

        root.getChildren().addAll(hdr, lblStatus, new Separator(), grid, new Separator(), lblDescHdr, lblDesc);
        if (!noteBox.getChildren().isEmpty()) root.getChildren().add(noteBox);
        root.getChildren().addAll(imgHdr, gallery, closeRow);

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #160f29; -fx-background-color: #160f29;");

        dlg.setScene(new Scene(sp, 660, 620));
        dlg.show();
    }

    // ---------------------------------------------------------- تب کاربران

    /**
     * کامپوننت/ساختار «users tab» را می‌سازد و برمی‌گرداند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildUsersTab() {
        usersBox = new VBox(10);

        Button btnRefresh = new Button("\ud83d\udd04 بروزرسانی لیست کاربران");
        btnRefresh.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnRefresh.setOnAction(e -> loadUsers());

        HBox controls = new HBox(10, btnRefresh);
        controls.setPadding(new Insets(10, 15, 0, 15));

        VBox container = new VBox(10, controls, wrapScroll(usersBox));
        VBox.setVgrow(container.getChildren().get(1), Priority.ALWAYS);
        container.setStyle("-fx-background-color: #160f29;");
        return container;
    }

    /**
     * داده‌های مربوط به «users» را بارگذاری و نمایش می‌دهد.
     */
    private void loadUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/users"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    usersBox.getChildren().clear();
                    if (response.statusCode() != 200) {
                        usersBox.getChildren().add(mutedLabel("خطا در دریافت کاربران (کد " + response.statusCode() + ")"));
                        return;
                    }
                    try {
                        JsonNode data = mapper.readTree(response.body()).path("data");
                        for (JsonNode user : data) {
                            usersBox.getChildren().add(buildUserCard(user));
                        }
                    } catch (Exception ex) {
                        usersBox.getChildren().add(mutedLabel("خطا در پردازش اطلاعات."));
                    }
                }));
    }

    /**
     * کامپوننت/ساختار «user card» را می‌سازد و برمی‌گرداند.
     *
     * @param user کاربر
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildUserCard(JsonNode user) {
        long id = user.path("id").asLong();
        String role = user.path("role").asText("USER");
        String status = user.path("status").asText("ACTIVE");

        VBox card = createCard();
        card.getChildren().add(infoLabel("\ud83d\udc64 " + user.path("name").asText("-")));
        card.getChildren().add(mutedLabel("نام کاربری: " + user.path("username").asText("-")));
        card.getChildren().add(mutedLabel("شماره: " + user.path("phoneNumber").asText("-")
                + "  |  ایمیل: " + user.path("email").asText("-")
                + "  |  نقش: " + role
                + "  |  وضعیت: " + ("BLOCKED".equalsIgnoreCase(status) ? "مسدود" : "فعال")));

        if (!"ADMIN".equalsIgnoreCase(role)) {
            HBox actions = new HBox(10);
            if ("BLOCKED".equalsIgnoreCase(status)) {
                Button btnUnblock = new Button("\ud83d\udd13 رفع مسدودی");
                btnUnblock.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
                btnUnblock.setOnAction(e -> postAdminAction("/api/admin/users/" + id + "/unblock", null, this::loadUsers));
                actions.getChildren().add(btnUnblock);
            } else {
                Button btnBlock = new Button("\ud83d\udeab مسدود کردن");
                btnBlock.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
                btnBlock.setOnAction(e -> postAdminAction("/api/admin/users/" + id + "/block", null, this::loadUsers));
                actions.getChildren().add(btnBlock);
            }
            card.getChildren().add(actions);
        }

        return card;
    }

    // ---------------------------------------------------------- تب دسته‌بندی‌ها

    // زیردسته‌بندی: کمبوی انتخاب والد + نگاشت نام والد به شناسه
    private javafx.scene.control.ComboBox<String> categoryParentCombo;
    private final java.util.Map<String, Long> parentCategoryIds = new java.util.LinkedHashMap<>();

    /**
     * کامپوننت/ساختار «categories tab» را می‌سازد و برمی‌گرداند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildCategoriesTab() {
        categoriesBox = new VBox(10);

        TextField txtName = new TextField();
        txtName.setPromptText("نام دسته‌بندی جدید...");
        txtName.setStyle("-fx-background-color: #241942; -fx-text-fill: white; -fx-prompt-text-fill: #b9a6df; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn';");
        HBox.setHgrow(txtName, Priority.ALWAYS);

        Button btnAdd = new Button("➕ افزودن");
        btnAdd.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnAdd.setOnAction(e -> {
            String name = txtName.getText() == null ? "" : txtName.getText().trim();
            if (name.isEmpty()) {
                return;
            }
            String sel = categoryParentCombo.getValue();
            Long parentId = (sel == null || "— دسته اصلی —".equals(sel)) ? null : parentCategoryIds.get(sel);
            String body = "{\"name\":\"" + name.replace("\"", "\\\"") + "\""
                    + (parentId != null ? ",\"parentId\":" + parentId : "") + "}";
            postAdminAction("/api/admin/categories", body, () -> {
                txtName.clear();
                categoryParentCombo.setValue("— دسته اصلی —");
                loadCategories();
            });
        });

        // انتخاب دسته والد برای ساخت زیردسته (پیش‌فرض: دسته اصلی)
        categoryParentCombo = new javafx.scene.control.ComboBox<>();
        categoryParentCombo.getItems().add("— دسته اصلی —");
        categoryParentCombo.setValue("— دسته اصلی —");
        categoryParentCombo.setPrefWidth(160);
        categoryParentCombo.setMinWidth(130);
        categoryParentCombo.setStyle(
            "-fx-background-color: #3b286b; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn'; -fx-text-fill: white; -fx-prompt-text-fill: #b9a6df; -fx-font-size: 13px;");
        // فقط لیست بازشونده را تم می‌کنیم — شکل دکمه دست نخورده نمی‌شود
        categoryParentCombo.setCellFactory(lv -> {
            lv.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b;");
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            final String norm = "-fx-background-color: #241942; -fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn'; -fx-padding: 7 12 7 12;";
            final String hovr = "-fx-background-color: #3b286b; -fx-text-fill: #ffc83b; -fx-font-family: 'Vazirmatn'; -fx-padding: 7 12 7 12;";
            cell.setStyle(norm);
            cell.setOnMouseEntered(e -> cell.setStyle(hovr));
            cell.setOnMouseExited(e -> cell.setStyle(norm));
            return cell;
        });

        // متن مقدار انتخاب‌شده روی دکمه کمبو باید سفید باشد (پیش‌فرض مشکی روی زمینه تیره خوانا نبود)
        javafx.scene.control.ListCell<String> parentBtnCell = new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? categoryParentCombo.getPromptText() : item);
            }
        };
        parentBtnCell.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");
        categoryParentCombo.setButtonCell(parentBtnCell);

        HBox controls = new HBox(10, txtName, categoryParentCombo, btnAdd);
        controls.setPadding(new Insets(10, 15, 0, 15));

        VBox container = new VBox(10, controls, wrapScroll(categoriesBox));
        VBox.setVgrow(container.getChildren().get(1), Priority.ALWAYS);
        container.setStyle("-fx-background-color: #160f29;");
        return container;
    }

    /**
     * داده‌های مربوط به «categories» را بارگذاری و نمایش می‌دهد.
     */
    private void loadCategories() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/categories"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    categoriesBox.getChildren().clear();
                    if (response.statusCode() != 200) {
                        categoriesBox.getChildren().add(mutedLabel("خطا در دریافت دسته‌بندی‌ها (کد " + response.statusCode() + ")"));
                        return;
                    }
                    try {
                        JsonNode data = mapper.readTree(response.body()).path("data");

                        // تفکیک دسته‌های اصلی و زیردسته‌ها
                        java.util.List<JsonNode> parents = new java.util.ArrayList<>();
                        java.util.Map<Long, java.util.List<JsonNode>> childrenOf = new java.util.LinkedHashMap<>();
                        for (JsonNode category : data) {
                            if (category.hasNonNull("parentId")) {
                                childrenOf.computeIfAbsent(category.path("parentId").asLong(),
                                        k -> new java.util.ArrayList<>()).add(category);
                            } else {
                                parents.add(category);
                            }
                        }

                        // به‌روزرسانی لیست والدها در کمبوی افزودن زیردسته
                        parentCategoryIds.clear();
                        for (JsonNode parent : parents) {
                            parentCategoryIds.put(parent.path("name").asText("-"), parent.path("id").asLong());
                        }
                        if (categoryParentCombo != null) {
                            String prevSel = categoryParentCombo.getValue();
                            categoryParentCombo.getItems().setAll("— دسته اصلی —");
                            categoryParentCombo.getItems().addAll(parentCategoryIds.keySet());
                            categoryParentCombo.setValue(
                                    categoryParentCombo.getItems().contains(prevSel) ? prevSel : "— دسته اصلی —");
                        }

                        for (JsonNode parent : parents) {
                            addCategoryRow(parent, false);
                            for (JsonNode child : childrenOf.getOrDefault(parent.path("id").asLong(),
                                    java.util.Collections.emptyList())) {
                                addCategoryRow(child, true);
                            }
                        }
                    } catch (Exception ex) {
                        categoriesBox.getChildren().add(mutedLabel("خطا در پردازش اطلاعات."));
                    }
                }));
    }

    // ساخت ردیف یک دسته‌بندی (زیردسته‌ها با علامت و تورفتگی)
    /**
     * «category row» را اضافه می‌کند.
     *
     * @param category دسته‌بندی
     * @param isChild پارامتر isChild
     */
    private void addCategoryRow(JsonNode category, boolean isChild) {
        long id = category.path("id").asLong();
        String catName = category.path("name").asText("-");

        Label lblName = new Label((isChild ? "↳ " : "\ud83d\uddc2\ufe0f ") + catName);
        lblName.setStyle("-fx-text-fill: " + (isChild ? "#b9a6df" : "white") + "; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: " + (isChild ? "#1d1436" : "#241942") + "; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-radius: 10;");
        if (isChild) {
            // تورفتگی ردیف زیردسته زیر دسته والد
            VBox.setMargin(row, new Insets(0, 0, 0, 30));
        }

        Button btnEdit = new Button("✏️ ویرایش");
        btnEdit.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnEdit.setOnAction(e -> showInlineRename(row, catName,
                newName -> putAdminAction("/api/admin/categories/" + id,
                        "{\"name\":\"" + newName.replace("\"", "\\\"") + "\"}",
                        this::loadCategories),
                this::loadCategories));

        Button btnDelete = new Button("\ud83d\uddd1\ufe0f حذف");
        btnDelete.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnDelete.setOnAction(e -> deleteAdminAction("/api/admin/categories/" + id, this::loadCategories));

        row.getChildren().addAll(lblName, spacer, btnEdit, btnDelete);
        categoriesBox.getChildren().add(row);
    }

    // ---------------------------------------------------------- تب شهرها

    /**
     * کامپوننت/ساختار «cities tab» را می‌سازد و برمی‌گرداند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildCitiesTab() {
        citiesBox = new VBox(10);

        TextField txtName = new TextField();
        txtName.setPromptText("نام شهر جدید...");
        txtName.setStyle("-fx-background-color: #241942; -fx-text-fill: white; -fx-prompt-text-fill: #b9a6df; -fx-background-radius: 8; -fx-font-family: 'Vazirmatn';");
        HBox.setHgrow(txtName, Priority.ALWAYS);

        Button btnAdd = new Button("➕ افزودن");
        btnAdd.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnAdd.setOnAction(e -> {
            String name = txtName.getText() == null ? "" : txtName.getText().trim();
            if (name.isEmpty()) {
                return;
            }
            String body = "{\"name\":\"" + name.replace("\"", "\\\"") + "\"}";
            postAdminAction("/api/admin/cities", body, () -> {
                txtName.clear();
                loadCities();
            });
        });

        HBox controls = new HBox(10, txtName, btnAdd);
        controls.setPadding(new Insets(10, 15, 0, 15));

        VBox container = new VBox(10, controls, wrapScroll(citiesBox));
        VBox.setVgrow(container.getChildren().get(1), Priority.ALWAYS);
        container.setStyle("-fx-background-color: #160f29;");
        return container;
    }

    /**
     * داده‌های مربوط به «cities» را بارگذاری و نمایش می‌دهد.
     */
    private void loadCities() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/cities"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    citiesBox.getChildren().clear();
                    if (response.statusCode() != 200) {
                        citiesBox.getChildren().add(mutedLabel("خطا در دریافت شهرها (کد " + response.statusCode() + ")"));
                        return;
                    }
                    try {
                        JsonNode data = mapper.readTree(response.body()).path("data");
                        for (JsonNode city : data) {
                            long id = city.path("id").asLong();
                            String cityName = city.path("name").asText("-");

                            Label lblName = new Label("\ud83c\udfd9\ufe0f " + cityName);
                            lblName.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

                            HBox row = new HBox(10);
                            row.setAlignment(Pos.CENTER_LEFT);
                            row.setPadding(new Insets(10));
                            row.setStyle("-fx-background-color: #241942; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-radius: 10;");

                            Button btnEdit = new Button("✏️ ویرایش");
                            btnEdit.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
                            btnEdit.setOnAction(e -> showInlineRename(row, cityName,
                                    newName -> putAdminAction("/api/admin/cities/" + id,
                                            "{\"name\":\"" + newName.replace("\"", "\\\"") + "\"}",
                                            this::loadCities),
                                    this::loadCities));

                            Button btnDelete = new Button("\ud83d\uddd1\ufe0f حذف");
                            btnDelete.setStyle("-fx-background-color: #b71c1c; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
                            btnDelete.setOnAction(e -> deleteAdminAction("/api/admin/cities/" + id, this::loadCities));

                            row.getChildren().addAll(lblName, spacer, btnEdit, btnDelete);
                            citiesBox.getChildren().add(row);
                        }
                    } catch (Exception ex) {
                        citiesBox.getChildren().add(mutedLabel("خطا در پردازش اطلاعات."));
                    }
                }));
    }

    // ✏️ ویرایش درجا: تبدیل ردیف به فیلد متنی + دکمه ثبت/انصراف
    /**
     * «inline rename» را به کاربر نمایش می‌دهد.
     *
     * @param row پارامتر row
     * @param oldName پارامتر oldName
     * @param onSave پارامتر onSave
     * @param onCancel پارامتر onCancel
     */
    private void showInlineRename(HBox row, String oldName,
                                  java.util.function.Consumer<String> onSave,
                                  Runnable onCancel) {
        TextField txt = new TextField(oldName);
        txt.setStyle("-fx-background-color: #160f29; -fx-text-fill: white; -fx-background-radius: 8; -fx-border-color: #ffc83b; -fx-border-radius: 8; -fx-font-family: 'Vazirmatn';");
        HBox.setHgrow(txt, Priority.ALWAYS);

        Button btnSave = new Button("✔ ثبت");
        btnSave.setStyle("-fx-background-color: #ffc83b; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnSave.setOnAction(e -> {
            String newName = txt.getText() == null ? "" : txt.getText().trim();
            if (newName.isEmpty() || newName.equals(oldName)) {
                onCancel.run();
                return;
            }
            onSave.accept(newName);
        });

        Button btnCancel = new Button("✖ انصراف");
        btnCancel.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnCancel.setOnAction(e -> onCancel.run());

        row.getChildren().setAll(txt, btnSave, btnCancel);
    }

    // درخواست PUT برای ویرایش (دسته‌بندی/شهر)
    /**
     * متد «putAdminAction»؛ بخشی از عملکرد کلاس AdminPanelController را پیاده‌سازی می‌کند.
     *
     * @param path پارامتر path
     * @param jsonBody پارامتر jsonBody
     * @param onSuccess پارامتر onSuccess
     */
    private void putAdminAction(String path, String jsonBody, Runnable onSuccess) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        onSuccess.run();
                    } else {
                        showError(response.body(), response.statusCode());
                    }
                }));
    }

    // ---------------------------------------------------------- تب آمار

    /**
     * کامپوننت/ساختار «stats tab» را می‌سازد و برمی‌گرداند.
     *
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildStatsTab() {
        statsBox = new VBox(10);

        Button btnRefresh = new Button("\ud83d\udd04 بروزرسانی آمار");
        btnRefresh.setStyle("-fx-background-color: #3b286b; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-family: 'Vazirmatn';");
        btnRefresh.setOnAction(e -> loadStats());

        HBox controls = new HBox(10, btnRefresh);
        controls.setPadding(new Insets(10, 15, 0, 15));

        VBox container = new VBox(10, controls, wrapScroll(statsBox));
        VBox.setVgrow(container.getChildren().get(1), Priority.ALWAYS);
        container.setStyle("-fx-background-color: #160f29;");
        return container;
    }

    /**
     * داده‌های مربوط به «stats» را بارگذاری و نمایش می‌دهد.
     */
    private void loadStats() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/admin/stats"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    statsBox.getChildren().clear();
                    if (response.statusCode() != 200) {
                        statsBox.getChildren().add(mutedLabel("خطا در دریافت آمار (کد " + response.statusCode() + ")"));
                        return;
                    }
                    try {
                        JsonNode data = mapper.readTree(response.body()).path("data");
                        statsBox.getChildren().add(buildStatRow("\ud83d\udc65 تعداد کل کاربران", data.path("totalUsers").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\ud83d\udeab کاربران مسدود", data.path("blockedUsers").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\ud83c\udff7\ufe0f تعداد کل آگهی‌ها", data.path("totalAds").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\ud83d\udd52 در انتظار تایید", data.path("pendingAds").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\ud83d\udfe2 آگهی‌های فعال", data.path("activeAds").asText("0")));
                        statsBox.getChildren().add(buildStatRow("❌ آگهی‌های رد شده", data.path("rejectedAds").asText("0")));
                        statsBox.getChildren().add(buildStatRow("✅ فروخته شده", data.path("soldAds").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\ud83d\udcac تعداد گفتگوها", data.path("totalConversations").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\u2709\ufe0f تعداد پیام‌ها", data.path("totalMessages").asText("0")));
                        statsBox.getChildren().add(buildStatRow("\u2b50 تعداد امتیازها", data.path("totalRatings").asText("0")));
                    } catch (Exception ex) {
                        statsBox.getChildren().add(mutedLabel("خطا در پردازش اطلاعات."));
                    }
                }));
    }

    /**
     * کامپوننت/ساختار «stat row» را می‌سازد و برمی‌گرداند.
     *
     * @param title عنوان
     * @param value مقدار
     * @return کامپوننت گرافیکی ساخته‌شده
     */
    private Node buildStatRow(String title, String value) {
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 13px; -fx-font-family: 'Vazirmatn';");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #ffc83b; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        HBox row = new HBox(10, lblTitle, spacer, lblValue);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: #241942; -fx-background-radius: 10; -fx-border-color: #3b286b; -fx-border-radius: 10;");
        return row;
    }

    // ---------------------------------------------------------- ابزارهای HTTP

    /**
     * متد «postAdminAction»؛ بخشی از عملکرد کلاس AdminPanelController را پیاده‌سازی می‌کند.
     *
     * @param path پارامتر path
     * @param jsonBody پارامتر jsonBody
     * @param onSuccess پارامتر onSuccess
     */
    private void postAdminAction(String path, String jsonBody, Runnable onSuccess) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + MainApplication.jwtToken);

        if (jsonBody != null) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, java.nio.charset.StandardCharsets.UTF_8));
        } else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }

        client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        onSuccess.run();
                    } else {
                        showError(response.body(), response.statusCode());
                    }
                }));
    }

    /**
     * «admin action» را حذف می‌کند.
     *
     * @param path پارامتر path
     * @param onSuccess پارامتر onSuccess
     */
    private void deleteAdminAction(String path, Runnable onSuccess) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .DELETE()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        onSuccess.run();
                    } else {
                        showError(response.body(), response.statusCode());
                    }
                }));
    }

    /**
     * «error» را به کاربر نمایش می‌دهد.
     *
     * @param responseBody پارامتر responseBody
     * @param statusCode پارامتر statusCode
     */
    private void showError(String responseBody, int statusCode) {
        String message = "عملیات ناموفق بود (کد " + statusCode + ")";
        try {
            JsonNode node = mapper.readTree(responseBody);
            if (node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        UiTheme.styleAlert(alert);
        alert.show();
    }
}
