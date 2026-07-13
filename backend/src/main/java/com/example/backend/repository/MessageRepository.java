package com.example.backend.repository;

import com.example.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 🟢 دریافت تاریخچه پیام‌های یک گفتگوی خاص به ترتیب زمان ارسال
     * این متد جایگزین متد قدیمی findChatHistory شده است.
     */
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);
}