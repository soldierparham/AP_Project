package com.example.backend.repository;

import com.example.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ریپازیتوری JPA پیام‌ها؛ کوئری‌های دریافت پیام‌های یک مکالمه.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * دریافت تاریخچه پیام‌های یک مکالمه بر اساس زمان ارسال
     * این متد جایگزین قدیمی findChatHistory شده است.
     */
    List<Message> findByConversationIdOrderByTimestampAsc(Long conversationId);

    // 🔧 جدید: حذف همه پیام‌های یک مکالمه (برای cascade delete هنگام حذف آگهی)
    /**
     * «by conversation id» را حذف می‌کند.
     *
     * @param conversationId شناسه مکالمه
     */
    void deleteByConversationId(Long conversationId);

    // آخرین پیام یک مکالمه
    /**
     * «top by conversation id order by timestamp desc» را جستجو و پیدا می‌کند.
     *
     * @param conversationId شناسه مکالمه
     * @return مقدار بازگشتی
     */
    java.util.Optional<Message> findTopByConversationIdOrderByTimestampDesc(Long conversationId);
}
