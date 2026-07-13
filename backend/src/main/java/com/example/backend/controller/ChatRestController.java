package com.example.backend.controller;

import com.example.backend.dto.MessageRequest;
import com.example.backend.model.Advertisement;
import com.example.backend.model.Conversation;
import com.example.backend.model.Message;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    /**
     * 📥 ۱. شروع گفتگو یا دریافت گفتگوی موجود
     */
    @Transactional
    @PostMapping("/conversations/start")
    public ResponseEntity<?> startConversation(@RequestParam Long adId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("کاربر احراز هویت نشده است.");
        }

        String currentUser = principal.getName();
        System.out.println("🔍 [BACKEND] شروع چت برای آگهی: " + adId + " توسط کاربر: " + currentUser);

        Advertisement ad = advertisementRepository.findById(adId)
                .orElseThrow(() -> new RuntimeException("آگهی مورد نظر پیدا نشد."));

        String seller = ad.getOwnerUsername();

        if (currentUser.equalsIgnoreCase(seller)) {
            return ResponseEntity.badRequest().body("شما نمی‌توانید با خودتان درباره آگهی‌تان چت کنید!");
        }

        Conversation conversation = conversationRepository
                .findByAdvertisementIdAndBuyerUsernameAndSellerUsername(adId, currentUser, seller)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation(ad, currentUser, seller);
                    return conversationRepository.save(newConv);
                });

        return ResponseEntity.ok(conversation);
    }

    /**
     * 📥 ۲. دریافت تمام پیام‌های یک گفتگوی خاص
     */
    @GetMapping("/messages")
    public ResponseEntity<List<Message>> getMessages(@RequestParam Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 📥 ۳. ارسال پیام جدید درون یک گفتگوی مشخص (نسخه عیب‌یاب)
     */
    @Transactional
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody MessageRequest request, Principal principal) {
        System.out.println("\n📬 [BACKEND DEBUG] ======= درخواست ارسال پیام جدید دریافت شد =======");

        if (principal == null) {
            System.err.println("❌ [BACKEND DEBUG] خطا: شیء Principal خالی است! کاربر توکن معتبر ارسال نکرده است.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("کاربر احراز هویت نشده است.");
        }

        String senderName = principal.getName();
        System.out.println("👤 [BACKEND DEBUG] فرستنده پیام: " + senderName);

        if (request == null) {
            System.err.println("❌ [BACKEND DEBUG] خطا: شیء MessageRequest از سمت فرانت‌اند NULL ارسال شده است!");
            return ResponseEntity.badRequest().body("بدنه درخواست خالی است.");
        }

        // 🔍 چاپ مقادیر دریافتی برای بررسی صحت عملکرد Jackson/Lombok
        System.out.println("📦 [BACKEND DEBUG] مقادیر استخراج شده از درخواست فرانت‌اند:");
        System.out.println("   - شناسه گفتگو (ConversationID): " + request.getConversationId());
        System.out.println("   - متن پیام (Content): \"" + request.getContent() + "\"");

        try {
            if (request.getConversationId() == null) {
                throw new IllegalArgumentException("شناسه گفتگو (ConversationId) نمی‌تواند Null باشد.");
            }

            // پیدا کردن گفتگوی مربوطه از روی دیتابیس
            Conversation conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> {
                        System.err.println("❌ [BACKEND DEBUG] خطا: گفتگویی با شناسه " + request.getConversationId() + " در دیتابیس یافت نشد!");
                        return new RuntimeException("گفتگوی مورد نظر یافت نشد.");
                    });

            System.out.println("✅ [BACKEND DEBUG] گفتگو در دیتابیس با موفقیت پیدا شد.");

            // ساخت و انتساب پیام به گفتگو
            Message newMessage = new Message();
            newMessage.setConversation(conversation);
            newMessage.setSenderUsername(senderName);
            newMessage.setContent(request.getContent());
            newMessage.setTimestamp(LocalDateTime.now());

            // ذخیره در دیتابیس
            // نکته: در صورت استفاده از JpaRepository، پیشنهاد می‌شود از saveAndFlush استفاده کنید تا خطاهای احتمالی دیتابیس فوراً پرتاب شوند.
            messageRepository.save(newMessage);

            System.out.println("💾 [BACKEND DEBUG] پیام با موفقیت در دیتابیس ثبت شد!");
            System.out.println("📬 [BACKEND DEBUG] ===================================================\n");

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            System.err.println("❌ [BACKEND DEBUG ERROR] خطا در ثبت پیام در دیتابیس:");
            e.printStackTrace(); // چاپ ریزترین جزییات خطا و StackTrace در ترمینال بک‌اند
            System.err.println("📬 [BACKEND DEBUG] ===================================================\n");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("خطا در ذخیره‌سازی پیام: " + e.getMessage());
        }
    }

    /**
     * 📥 ۴. دریافت تمام گفتگوهای اخیر کاربر فعلی
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<Conversation>> getConversations(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String currentUser = principal.getName();
        List<Conversation> conversations = conversationRepository
                .findByBuyerUsernameOrSellerUsername(currentUser, currentUser);
        return ResponseEntity.ok(conversations);
    }

    /**
     * 🔍 متد دیباگ برای مشاهده کل پیام‌های دیتابیس
     */
    @GetMapping("/all-debug")
    public List<Message> getAllMessagesDebug() {
        return messageRepository.findAll();
    }
}