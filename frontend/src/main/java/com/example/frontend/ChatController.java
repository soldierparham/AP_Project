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

public class ChatController {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient client = HttpClient.newHttpClient();

    private VBox contactsContainer;
    private VBox messagesContainer;
    private ScrollPane messagesScrollPane;
    private Label lblActiveChatUser;
    private TextField txtMessageInput;
    private Button btnSend;

    // 🔑 متغیرهای ردیابی موقعیت گفتگو
    private Long currentConversationId = null;
    private String currentChatUser = null;
    private String myUsername = null;
    private Timeline autoRefreshTimeline;
    private int conversationPollCounter = 0;

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
        txtMessageInput.setOnAction(e -> handleSendMessage());

        btnSend = new Button("ارسال 🚀");
        btnSend.setStyle("-fx-background-color: #ffc83b; -fx-padding: 10px 20px; -fx-background-radius: 6px; -fx-text-fill: #160f29; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSend.setDisable(true);
        btnSend.setOnAction(e -> handleSendMessage());

        inputBar.getChildren().addAll(txtMessageInput, btnSend);
        chatArea.getChildren().addAll(chatHeader, messagesScrollPane, inputBar);
        root.getChildren().addAll(sidebar, chatArea);

        System.out.println("🔍 [DEBUG] شروع ساخت لایه چت...");
        extractMyUsername();
        loadRealConversations();
        startLiveChatPolling();
        return root;
    }

    // --- شروع چت با فرستادن شناسه آگهی ---
    public HBox createChatView(Long advertisementId, String targetUsername) {
        System.out.println("🔍 [DEBUG] متد چت مستقیم با آگهی فراخوانی شد. شناسه آگهی: " + advertisementId + "، مخاطب هدف: " + targetUsername);
        HBox root = createChatView();

        if (MainApplication.jwtToken == null) {
            System.err.println("❌ [DEBUG ERROR] توکن JWT خالی است! (MainApplication.jwtToken = null)");
            lblActiveChatUser.setText("⚠️ توکن امنیتی وجود ندارد. ابتدا لاگین کنید.");
            return root;
        }

        if (advertisementId != null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/chat/conversations/start?adId=" + advertisementId))
                    .header("Authorization", "Bearer " + MainApplication.jwtToken)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        System.out.println("🔍 [DEBUG] پاسخ سرور برای ساخت چت: کد وضعیت " + res.statusCode());
                        System.out.println("🔍 [DEBUG] بدنه پاسخ: " + res.body());

                        if (res.statusCode() == 200) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode node = mapper.readTree(res.body());
                                Long convId = node.path("id").asLong();
                                String adTitle = node.path("advertisement").path("title").asText("آگهی");

                                Platform.runLater(() -> {
                                    System.out.println("✅ [DEBUG] چت با موفقیت با شناسه گفتگو " + convId + " فعال شد.");
                                    selectConversation(convId, targetUsername, adTitle);
                                });
                            } catch (Exception e) {
                                System.err.println("❌ [DEBUG ERROR] خطا در پارس کردن جیسون پاسخ شروع چت: " + e.getMessage());
                            }
                        } else {
                            Platform.runLater(() -> {
                                lblActiveChatUser.setText("⚠️ خطا از سمت سرور: " + res.body());
                                txtMessageInput.setDisable(true);
                                btnSend.setDisable(true);
                            });
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("❌ [DEBUG ERROR] خطای شبکه در ارتباط با سرور: " + ex.getMessage());
                        return null;
                    });
        }
        return root;
    }

    public HBox createChatView(String targetUsername) {
        System.out.println("🔍 [DEBUG] متد قدیمی چت با نام کاربری فراخوانی شد: " + targetUsername);
        HBox root = createChatView();
        if (targetUsername != null && !targetUsername.isEmpty()) {
            this.currentChatUser = targetUsername;
            Platform.runLater(() -> {
                lblActiveChatUser.setText("💬 در حال جستجوی گفتگو با: " + targetUsername);
            });
            loadRealConversations();
        }
        return root;
    }

    private void extractMyUsername() {
        if (MainApplication.jwtToken == null) {
            System.err.println("⚠️ [DEBUG] توکن JWT برای استخراج نام کاربری خالی است.");
            return;
        }
        try {
            String[] parts = MainApplication.jwtToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(payload);
                myUsername = node.path("sub").asText("");
                System.out.println("🔑 [DEBUG] نام کاربری شما با موفقیت از JWT استخراج شد: " + myUsername);
            }
        } catch (Exception e) {
            System.err.println("❌ [DEBUG ERROR] خطا در رمزگشایی توکن: " + e.getMessage());
        }
    }

    private void loadChatMessagesFromServer(Long convId) {
        if (convId == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/messages?conversationId=" + convId))
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
                                String sender = msgNode.path("senderUsername").asText("unknown");
                                String content = msgNode.path("content").asText("");
                                boolean isMe = sender.trim().equalsIgnoreCase(myUsername);
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
                            System.err.println("❌ [DEBUG ERROR] خطای پارس کردن پیام‌ها: " + e.getMessage());
                        }
                    }
                });
    }

    private void handleSendMessage() {
        String text = txtMessageInput.getText().trim();
        if (text.isEmpty() || currentConversationId == null) return;

        String jsonBody = "{\"conversationId\":" + currentConversationId + ",\"content\":\"" + text.replace("\"", "\\\"") + "\"}";
        System.out.println("📤 [DEBUG] در حال ارسال پیام: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/send"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        txtMessageInput.clear();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    System.out.println("🔍 [DEBUG] نتیجه ارسال پیام: کد وضعیت " + res.statusCode());
                    if (res.statusCode() == 200) {
                        Platform.runLater(() -> {
                            loadChatMessagesFromServer(currentConversationId);
                            loadRealConversations();
                        });
                    }
                });
    }

    private void selectConversation(Long convId, String otherUsername, String adTitle) {
        System.out.println("🟢 [DEBUG] متد selectConversation اجرا شد. شناسه: " + convId + "، مخاطب: " + otherUsername);
        currentConversationId = convId;
        currentChatUser = otherUsername;
        lblActiveChatUser.setText("💬 گفتگو با: " + otherUsername + " (آگهی: " + adTitle + ")");

        // 🔓 فعال کردن فیلد متن و دکمه ارسال
        txtMessageInput.setDisable(false);
        btnSend.setDisable(false);

        loadChatMessagesFromServer(convId);
        Platform.runLater(this::loadRealConversations);
    }

    private void addContactRow(Long convId, String otherUsername, String adTitle) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(10, 10, 10, 10));

        if (currentConversationId != null && currentConversationId.equals(convId)) {
            row.setStyle("-fx-background-color: #241942; -fx-background-radius: 4px; -fx-cursor: hand;");
        } else {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 4px; -fx-cursor: hand;");
        }

        VBox textData = new VBox(4);
        Label lblUser = new Label("👤 " + otherUsername);
        lblUser.setStyle("-fx-text-fill: white; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label lblAd = new Label("📦 " + adTitle);
        lblAd.setStyle("-fx-text-fill: #b9a6df; -fx-font-family: 'Vazirmatn'; -fx-font-size: 11px;");

        textData.getChildren().addAll(lblUser, lblAd);
        row.getChildren().add(textData);

        row.setOnMouseEntered(e -> {
            if (currentConversationId == null || !currentConversationId.equals(convId)) {
                row.setStyle("-fx-background-color: #241942; -fx-cursor: hand; -fx-background-radius: 4px;");
            }
        });
        row.setOnMouseExited(e -> {
            if (currentConversationId == null || !currentConversationId.equals(convId)) {
                row.setStyle("-fx-background-color: transparent;");
            }
        });

        row.setOnMouseClicked(e -> selectConversation(convId, otherUsername, adTitle));
        contactsContainer.getChildren().add(0, row);
    }

    private void loadRealConversations() {
        if (MainApplication.jwtToken == null) return;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/chat/conversations"))
                .header("Authorization", "Bearer " + MainApplication.jwtToken)
                .GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> {
                    if (res.statusCode() == 200) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode root = mapper.readTree(res.body());
                            System.out.println("🔍 [DEBUG] گفتگوهای دریافت شده از سرور: " + root.toString());

                            Platform.runLater(() -> {
                                contactsContainer.getChildren().clear();
                                boolean foundActiveChat = false;
                                Long firstConvId = null;
                                String firstOtherUser = "";
                                String firstAdTitle = "";

                                if (root.isArray() && root.size() > 0) {
                                    for (JsonNode node : root) {
                                        Long convId = node.path("id").asLong();
                                        String adTitle = node.path("advertisement").path("title").asText("آگهی");
                                        String buyer = node.path("buyerUsername").asText("");
                                        String seller = node.path("sellerUsername").asText("");

                                        String otherUser = "";
                                        if (myUsername != null) {
                                            otherUser = myUsername.equalsIgnoreCase(buyer) ? seller : buyer;
                                        } else {
                                            otherUser = buyer;
                                        }

                                        if (convId != 0 && !otherUser.isEmpty()) {
                                            addContactRow(convId, otherUser, adTitle);

                                            if (firstConvId == null) {
                                                firstConvId = convId;
                                                firstOtherUser = otherUser;
                                                firstAdTitle = adTitle;
                                            }

                                            if (currentConversationId != null && currentConversationId.equals(convId)) {
                                                foundActiveChat = true;
                                            }

                                            if (currentConversationId == null && currentChatUser != null && currentChatUser.equalsIgnoreCase(otherUser)) {
                                                selectConversation(convId, otherUser, adTitle);
                                                foundActiveChat = true;
                                            }
                                        }
                                    }

                                    if (currentConversationId == null && firstConvId != null && !foundActiveChat && currentChatUser == null) {
                                        System.out.println("🟢 [DEBUG] هیچ چتی فعال نبود. به صورت خودکار اولین چت انتخاب شد.");
                                        selectConversation(firstConvId, firstOtherUser, firstAdTitle);
                                    }

                                } else {
                                    System.out.println("⚠️ [DEBUG] لیست چت‌های دریافتی خالی است.");
                                    lblActiveChatUser.setText("💬 هنوز گفتگویی شروع نشده است.");
                                    txtMessageInput.setDisable(true);
                                    btnSend.setDisable(true);
                                }
                            });
                        } catch (Exception e) {
                            System.err.println("❌ [DEBUG ERROR] خطای پارس کردن کل گفتگوها: " + e.getMessage());
                        }
                    } else {
                        System.err.println("❌ [DEBUG ERROR] خطا در دریافت گفتگوها. کد خطا: " + res.statusCode());
                    }
                });
    }

    private HBox createMessageBubbleNode(String text, boolean isMe) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setMaxWidth(320);
        lbl.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        String bgColor = isMe ? "#ffc83b" : "#241942";
        String textColor = isMe ? "#160f29" : "white";
        String borderRadius = isMe ? "12px 12px 0px 12px" : "12px 12px 12px 0px";

        lbl.setStyle("-fx-background-color: " + bgColor +
                "; -fx-text-fill: " + textColor +
                "; -fx-padding: 10px 14px; -fx-background-radius: " + borderRadius +
                "; -fx-font-family: 'Vazirmatn'; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isMe) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(spacer, lbl);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(lbl, spacer);
        }

        return row;
    }

    private void startLiveChatPolling() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            if (currentConversationId != null) {
                loadChatMessagesFromServer(currentConversationId);
            }

            conversationPollCounter++;
            if (conversationPollCounter >= 3) {
                loadRealConversations();
                conversationPollCounter = 0;
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }
}