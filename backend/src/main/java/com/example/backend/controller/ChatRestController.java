package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Conversation;
import com.example.backend.model.Message;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 💬 چت غیرهمزمان بین خریدار و فروشنده — نسخه تکمیل‌شده:
 * 🚫 کاربران مسدودشده اجازه شروع گفتگو یا ارسال پیام ندارند (مطابق سند پروژه)
 * 🚫 جدید: با کاربر مسدودشده هم نمی‌توان گفتگو شروع کرد یا به او پیام فرستاد
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 🚫 بررسی مسدود بودن کاربر
     */
    private boolean isBlocked(String username) {
        return userRepository.findByUsername(username)
                .map(u -> "BLOCKED".equalsIgnoreCase(u.getStatus()))
                .orElse(false);
    }

    /**
     * 🔍 شروع (یا ادامه) یک گفتگو برای یک آگهی — هر (خریدار، فروشنده، آگهی) فقط یک گفتگو دارد
     */
    @PostMapping("/start")
    public ResponseEntity startConversation(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        String buyerUsername = principal.getName();

        // 🚫 کاربر مسدودشده اجازه چت ندارد
        if (isBlocked(buyerUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "حساب کاربری شما مسدود شده است و امکان گفتگو ندارید.", "status", 403));
        }

        if (body == null || body.get("adId") == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شناسه آگهی (adId) الزامی است.", "status", 400));
        }

        long adId;
        try {
            adId = Long.parseLong(body.get("adId").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شناسه آگهی نامعتبر است.", "status", 400));
        }

        Optional adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        Advertisement ad = (Advertisement) adOpt.get();
        String sellerUsername = ad.getOwnerUsername();

        if (buyerUsername.equals(sellerUsername)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شما نمی‌توانید برای آگهی خودتان گفتگو شروع کنید.", "status", 400));
        }

        // 🚫 جدید: امکان شروع گفتگو با فروشنده مسدودشده وجود ندارد
        if (isBlocked(sellerUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "این کاربر توسط مدیر مسدود شده است و امکان گفتگو با او وجود ندارد.", "status", 403));
        }

        Conversation conversation = conversationRepository
                .findByAdvertisementIdAndBuyerUsernameAndSellerUsername(adId, buyerUsername, sellerUsername)
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(ad, buyerUsername, sellerUsername)));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "conversationId", conversation.getId(),
                "adId", adId,
                "buyerUsername", buyerUsername,
                "sellerUsername", sellerUsername
        ));
    }

    /**
     * 📚 لیست گفتگوهای کاربر جاری (چه به عنوان خریدار چه فروشنده)
     */
    @GetMapping("/conversations")
    public ResponseEntity getMyConversations(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        String username = principal.getName();
        List<Conversation> conversations = conversationRepository
                .findByBuyerUsernameOrSellerUsername(username, username);

        // خروجی مسطح برای فرانت‌اند (بدون آبجکت تودرتوی آگهی)
        // 🔧 اگر آگهی حذف شده باشد، از adTitleSnapshot استفاده می‌شود
        List<Map<String, Object>> data = new ArrayList<>();
        for (Conversation c : conversations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            boolean adExists = c.getAdvertisement() != null;
            item.put("adId", adExists ? c.getAdvertisement().getId() : null);
            // عنوان: اگر آگهی موجود است از آن بخوان، وگرنه از snapshot ذخیره‌شده استفاده کن
            String adTitle = adExists
                    ? c.getAdvertisement().getTitle()
                    : (c.getAdTitleSnapshot() != null ? c.getAdTitleSnapshot() + " (حذف شده)" : "آگهی حذف شده");
            item.put("adTitle", adTitle);
            item.put("buyerUsername", c.getBuyerUsername());
            item.put("sellerUsername", c.getSellerUsername());
            messageRepository.findTopByConversationIdOrderByTimestampDesc(c.getId())
                .ifPresentOrElse(last -> {
                    item.put("lastMessageContent", last.getContent());
                    item.put("lastMessageSender", last.getSenderUsername());
                }, () -> {
                    item.put("lastMessageContent", "");
                    item.put("lastMessageSender", "");
                });
            data.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", data
        ));
    }

    /**
     * 💬 دریافت پیام‌های یک گفتگو (فقط طرفین گفتگو)
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity getMessages(@PathVariable Long conversationId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        Optional convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "گفتگوی مورد نظر یافت نشد.", "status", 404));
        }

        Conversation conversation = (Conversation) convOpt.get();
        String username = principal.getName();

        if (!username.equals(conversation.getBuyerUsername())
                && !username.equals(conversation.getSellerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "شما به این گفتگو دسترسی ندارید.", "status", 403));
        }

        List<Message> messages = messageRepository
                .findByConversationIdOrderByTimestampAsc(conversationId);

        // 📌 خروجی مسطح برای فرانت‌اند (بدون آبجکت تودرتوی گفتگو و آگهی)
        // فرانت‌اند JSON را با regex ساده پارس می‌کند و آبجکت تو در تو باعث می‌شد پیام‌ها نمایش داده نشوند
        List<Map<String, Object>> data = new ArrayList<>();
        for (Message m : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("senderUsername", m.getSenderUsername());
            item.put("content", m.getContent());
            item.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
            data.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", data
        ));
    }

    /**
     * 📤 ارسال پیام در یک گفتگو (فقط طرفین گفتگو، کاربر مسدود ممنوع)
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> body,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        String username = principal.getName();

        // 🚫 کاربر مسدودشده اجازه ارسال پیام ندارد
        if (isBlocked(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "حساب کاربری شما مسدود شده است و امکان ارسال پیام ندارید.", "status", 403));
        }

        if (body == null || body.get("content") == null || body.get("content").toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "متن پیام نمی‌تواند خالی باشد.", "status", 400));
        }

        Optional convOpt = conversationRepository.findById(conversationId);
        if (convOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "گفتگوی مورد نظر یافت نشد.", "status", 404));
        }

        Conversation conversation = (Conversation) convOpt.get();

        if (!username.equals(conversation.getBuyerUsername())
                && !username.equals(conversation.getSellerUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "شما به این گفتگو دسترسی ندارید.", "status", 403));
        }

        // 🚫 جدید: ارسال پیام به کاربر مسدودشده ممنوع است
        String otherParty = username.equals(conversation.getBuyerUsername())
                ? conversation.getSellerUsername()
                : conversation.getBuyerUsername();
        if (isBlocked(otherParty)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "این کاربر توسط مدیر مسدود شده است و امکان ارسال پیام به او وجود ندارد.", "status", 403));
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUsername(username);
        message.setContent(body.get("content").toString());
        message.setTimestamp(LocalDateTime.now());

        messageRepository.save(message);

        return ResponseEntity.ok("OK");
    }
}
