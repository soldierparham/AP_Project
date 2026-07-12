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

    // --- نسخه اصلی لایه گرافیکی ---
    public HBox createChatView() {
        HBox root = new HBox();
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.setStyle("-fx-background-color: #160f29;");

        // سایدبار گفتگوها
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

        // هدر چت
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
        messagesContainer.setStyle("-fx-background-color: transparent;");
        messagesContainer.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        messagesScrollPane = new ScrollPane(messagesContainer);
        messagesScrollPane.setFitToWidth(true);
        messagesScrollPane.setStyle("-fx-background: #160f29; -fx-background-color: #160f29; -fx-border-color: transparent;");
        VBox.setVgrow(messagesScrollPane, Priority.ALWAYS);

        // بارگذاری اینپوت پیام
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

    // --- نسخه مستقیم چت با کاربر هدف ---
    public HBox createChatView(String targetUsername) {
        HBox root = createChatView();
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

    private void loadChatMessagesFromServer(String withUser) {
        if (withUser == null || MainApplication.jwtToken == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/messages?with=" + withUser))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(response.body());

                            if (!root.isArray()) return;

                            List<HBox> newBubbles = new ArrayList<>();
                            for (JsonNode msgNode : root) {
                                String sender = msgNode.path("senderUsername").asText("").isEmpty()
                                        ? msgNode.path("sender").asText("unknown")
                                        : msgNode.path("senderUsername").asText("unknown");

                                String content = msgNode.path("content").asText("");

                                // 🟢 قانون طلایی حذف واسطه: در چت با با کاربر Asd، اگر فرستنده Asd نباشد، قطعاً شما هستید!
                                boolean isMe = !sender.trim().equalsIgnoreCase(withUser.trim());

                                newBubbles.add(createMessageBubbleNode(content, isMe));
                            }

                            Platform.runLater(() -> {
                                messagesContainer.getChildren().clear();
                                if (newBubbles.isEmpty()) {
                                    Label emptyLabel = new Label("هنوز پیامی رد و بدل نشده...");
                                    emptyLabel.setStyle("-fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn';");
                                    messagesContainer.getChildren().add(emptyLabel);
                                } else {
                                    messagesContainer.getChildren().addAll(newBubbles);
                                }
                                messagesScrollPane.setVvalue(1.0);
                            });

                        } catch (Exception e) {
                            System.err.println("❌ خطایِ پردازشِ JSON: " + e.getMessage());
                        }
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
        row.setPadding(new Insets(12, 10, 12, 10));
        row.setStyle("-fx-cursor: hand; -fx-background-radius: 4px;");

        Label lblUser = new Label("👤 " + username);
        lblUser.setStyle("-fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");
        row.getChildren().add(lblUser);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #241942; -fx-cursor: hand; -fx-background-radius: 4px;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));

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
                            contactsContainer.getChildren().clear();
                            Matcher m = Pattern.compile("\"username\":\"([^\"]+)\"").matcher(res.body());
                            while(m.find()) addContactRow(m.group(1), false);
                        });
                    }
                });
    }

    // --- مدیریت مکانیکی و استایل‌دهی حباب‌ها ---
    private HBox createMessageBubbleNode(String text, boolean isMe) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        String bgColor = isMe ? "#ffc83b" : "#241942"; // طلایی برای شما، بنفش برای مخاطب
        String textColor = isMe ? "#160f29" : "white"; // متن تیره برای خوانایی روی طلایی

        lbl.setStyle("-fx-background-color: " + bgColor +
                "; -fx-text-fill: " + textColor +
                "; -fx-padding: 10px 14px; -fx-background-radius: 10px; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isMe) {
            // پیام شما: اسپیسر اول قرار می‌گیرد و حباب را به راست هل می‌دهد
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, lbl);
        } else {
            // پیام مخاطب: حباب اول قرار می‌گیرد و در چپ ثابت می‌ماند
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(lbl, spacer);
        }

        return row;
    }

    private void startLiveChatPolling() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentChatUser != null) loadChatMessagesFromServer(currentChatUser);
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }
}