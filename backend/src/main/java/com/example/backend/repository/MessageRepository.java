package com.example.backend.repository;

import com.example.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * دریافت تاریخچه پیام‌های یک مکالمه بر اساس زمان ارسال
     * این متد جایگزین قدیمی findChatHistory شده است.
     */
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);

    // 🔧 جدید: حذف همه پیام‌های یک مکالمه (برای cascade delete هنگام حذف آگهی)
    void deleteByConversationId(Long conversationId);
}
