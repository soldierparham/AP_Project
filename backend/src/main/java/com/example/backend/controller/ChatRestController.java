package com.example.backend.controller;

import com.example.backend.dto.MessageRequest;
import com.example.backend.model.Message;
import com.example.backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    @Autowired
    private MessageRepository messageRepository;

    // دریافت پیام‌های چت
    // اصلاح در getMessages
    @Autowired
    private com.example.backend.repository.UserRepository userRepository; // UserRepository را تزریق کنید

    @GetMapping("/messages")
    public List<Message> getMessages(@RequestParam String with, Principal principal) {
        String currentUsername = principal.getName(); // حالا دقیقاً "ali" برمی‌گردد
        return messageRepository.findChatHistory(currentUsername, with);
    }

    @Transactional
    @PostMapping("/send")
    public String sendMessage(@RequestBody MessageRequest request, Principal principal) {
        String senderName = principal.getName(); // درست شد!

        Message newMessage = new Message();
        newMessage.setSenderUsername(senderName);
        newMessage.setReceiverUsername(request.getReceiverUsername());
        newMessage.setContent(request.getContent());
        newMessage.setTimestamp(LocalDateTime.now());

        messageRepository.save(newMessage);
        return "OK";
    }

    // لیست گفتگوها
    @GetMapping("/conversations")
    public List<String> getConversations(Principal principal) {
        return messageRepository.findActiveConversations(principal.getName());
    }

    @GetMapping("/all-debug")
    public List<Message> getAllMessagesDebug() {
        return messageRepository.findAll();
    }
}