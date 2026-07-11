package com.example.frontend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatController {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient client = HttpClient.newHttpClient();

    private VBox contactsContainer;
    private VBox messagesContainer;
    private ScrollPane messagesScrollPane;
    private Label lblActiveChatUser;
    private TextField txtMessageInput;
    private Button btnSend;
    private String currentChatUser = null;
    private Timeline autoRefreshTimeline;
    private int lastLoadedMessageCount = -1;

    // --- نسخه اصلی (بدون ورودی) ---
    public HBox createChatView() {
        HBox root = new HBox();
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-background-color: #160f29;");

        // سایدبار
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(260);
        sidebar.setMinWidth(260);
        sidebar.setStyle("-fx-background-color: #1a1132; -fx-border-color: #3b286b; -fx-border-width: 0 0 0 1px;");
        sidebar.setPadding(new Insets(20, 15, 15, 15));

        Label lblTitle = new Label("📥 گفتگوهای اخیر");
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Vazirmatn';");

        ScrollPane contactsScrollPane = new ScrollPane();
        contactsContainer = new VBox(8);
        contactsScrollPane.setContent(contactsContainer);
        contactsScrollPane.setFitToWidth(true);
        contactsScrollPane.setStyle("-fx-background: #1a1132; -fx-background-color: #1a1132; -fx-border-color: transparent;");

        sidebar.getChildren().addAll(lblTitle, contactsScrollPane);
        VBox.setVgrow(contactsScrollPane, Priority.ALWAYS);

        // بخش اصلی چت
        VBox chatArea = new VBox();
        HBox.setHgrow(chatArea, Priority.ALWAYS);
        chatArea.setStyle("-fx-background-color: #160f29;");

        // هدر
        HBox chatHeader = new HBox(10);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(15, 20, 15, 20));
        chatHeader.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-width: 0 0 1px 0;");

        lblActiveChatUser = new Label("💬 یک گفتگو را انتخاب کنید");
        lblActiveChatUser.setStyle("-fx-text-fill: #b9a6df; -fx-font-size: 14px; -fx-font-family: 'Vazirmatn';");
        chatHeader.getChildren().add(lblActiveChatUser);

        // باکس پیام‌ها
        messagesContainer = new VBox(12);
        messagesContainer.setPadding(new Insets(20));
        messagesContainer.setFillWidth(true);

        messagesContainer.setStyle("-fx-background-color: red;");
        messagesContainer.getChildren().add(new Label("تست: آیا این باکس دیده می‌شود؟"));

        messagesScrollPane = new ScrollPane(messagesContainer);
        messagesScrollPane.setFitToWidth(true);
        messagesScrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29; -fx-border-color: transparent;");
        VBox.setVgrow(messagesScrollPane, Priority.ALWAYS);

        // اینپوت
        HBox inputBar = new HBox(10);
        inputBar.setPadding(new Insets(15, 20, 15, 20));
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.setStyle("-fx-background-color: #241942; -fx-border-color: #3b286b; -fx-border-width: 1px 0 0 0;");

        txtMessageInput = new TextField();
        txtMessageInput.setPromptText("پیام...");
        txtMessageInput.setStyle("-fx-background-color: #160f29; -fx-text-fill: white; -fx-border-color: #3b286b; -fx-border-radius: 6px; -fx-padding: 10px;");
        HBox.setHgrow(txtMessageInput, Priority.ALWAYS);
        txtMessageInput.setDisable(true);

        btnSend = new Button("ارسال 🚀");
        btnSend.setStyle("-fx-background-color: #ffc83b; -fx-padding: 10px 20px; -fx-background-radius: 6px;");
        btnSend.setDisable(true);
        btnSend.setOnAction(e -> handleSendMessage());

        inputBar.getChildren().addAll(txtMessageInput, btnSend);
        chatArea.getChildren().addAll(chatHeader, messagesScrollPane, inputBar);
        root.getChildren().addAll(sidebar, chatArea);

        loadRealConversations();
        startLiveChatPolling();
        return root;
    }

    // --- نسخه اضافه شده (رفع ارور شما) ---
    public HBox createChatView(String targetUsername) {
        HBox root = createChatView(); // فراخوانی نسخه اصلی برای ساخت UI
        if (targetUsername != null && !targetUsername.isEmpty()) {
            this.currentChatUser = targetUsername;
            Platform.runLater(() -> {
                lblActiveChatUser.setText("💬 گفتگو با: " + targetUsername);
                txtMessageInput.setDisable(false);
                btnSend.setDisable(false);
                loadChatMessagesFromServer(targetUsername);
            });
        }
        return root;
    }

    // --- سایر متدها ---
    private void loadChatMessagesFromServer(String withUser) {
        if (withUser == null || MainApplication.jwtToken == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/messages?with=" + withUser))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    // ۱. لاگ گرفتن از پاسخ خام برای بررسی
                    System.out.println("📥 وضعیت سرور: " + response.statusCode());
                    System.out.println("📥 پاسخ خام: " + response.body());

                    if (response.statusCode() == 200) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(response.body());

                            // ۲. چک کردن اینکه آیا اصلاً آرایه است یا خیر
                            if (!root.isArray()) {
                                System.err.println("⚠️ هشدار: خروجی سرور آرایه نیست! ساختار داده احتمالاً تغییر کرده.");
                                return;
                            }

                            List<HBox> newBubbles = new ArrayList<>();

                            for (JsonNode msgNode : root) {
                                // ۳. استفاده از path به جای get (امن‌تر در برابر NullPointerException)
                                String sender = msgNode.path("senderUsername").asText("unknown");
                                String content = msgNode.path("content").asText("بدون محتوا");

                                // لاگ برای دیدن اینکه داده‌ها چطور پردازش می‌شوند
                                System.out.println("📝 پردازش: فرستنده=" + sender + " | پیام=" + content);

                                boolean isMe = MainApplication.currentUsername != null &&
                                        sender.trim().equalsIgnoreCase(MainApplication.currentUsername.trim());

                                newBubbles.add(createMessageBubbleNode(content, isMe));
                            }

                            Platform.runLater(() -> {
                                messagesContainer.getChildren().clear();
                                if (newBubbles.isEmpty()) {
                                    messagesContainer.getChildren().add(new Label("هنوز پیامی رد و بدل نشده..."));
                                } else {
                                    messagesContainer.getChildren().addAll(newBubbles);
                                }
                                messagesScrollPane.setVvalue(1.0);
                            });

                        } catch (Exception e) {
                            System.err.println("❌ خطایِ پردازشِ JSON: " + e.getMessage());
                        }
                    } else {
                        System.err.println("❌ خطا در دریافت پیام‌ها. کد وضعیت: " + response.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("❌ خطای شبکه: " + ex.getMessage());
                    return null;
                });
    }

    private void handleSendMessage() {
        String text = txtMessageInput.getText().trim();
        if (text.isEmpty() || currentChatUser == null) return;
        String jsonBody = "{\"receiverUsername\":\"" + currentChatUser + "\",\"content\":\"" + text.replace("\"", "\\\"") + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/send"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        txtMessageInput.clear();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) {
                        lastLoadedMessageCount = -1;
                        Platform.runLater(() -> loadChatMessagesFromServer(currentChatUser));
                    }
                });
    }

    private void selectUserChat(String username, HBox row) {
        currentChatUser = username;
        lblActiveChatUser.setText("💬 " + username);
        txtMessageInput.setDisable(false);
        btnSend.setDisable(false);
        lastLoadedMessageCount = -1;
        loadChatMessagesFromServer(username);
    }

    private void addContactRow(String username, boolean autoSelect) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10));
        row.getChildren().add(new Label("👤 " + username));
        row.setOnMouseClicked(e -> selectUserChat(username, row));
        contactsContainer.getChildren().add(0, row);
    }

    private void loadRealConversations() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/conversations"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET().build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) {
                        Platform.runLater(() -> {
                            Matcher m = Pattern.compile("\"username\":\"([^\"]+)\"").matcher(res.body());
                            while(m.find()) addContactRow(m.group(1), false);
                        });
                    }
                });
    }

    private HBox createMessageBubbleNode(String text, boolean isMe) {
        HBox row = new HBox();
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: " + (isMe ? "#3b286b" : "#241942") + "; -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 10px;");
        row.setAlignment(isMe ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        row.getChildren().add(lbl);
        return row;
    }

    private void startLiveChatPolling() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentChatUser != null) loadChatMessagesFromServer(currentChatUser);
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private String extractJsonField(String source, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(source);
        return m.find() ? m.group(1) : "مشخص نشده";
    }
}