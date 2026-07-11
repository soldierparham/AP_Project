package com.example.backend.repository;

import com.example.backend.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // دریافت تاریخچه چت بین دو کاربر خاص به ترتیب زمان
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderUsername = :user1 AND m.receiverUsername = :user2) OR " +
            "(m.senderUsername = :user2 AND m.receiverUsername = :user1) " +
            "ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(@Param("user1") String user1, @Param("user2") String user2);

    // دریافت لیست کسانی که کاربر فعلی با آن‌ها چت کرده است
    @Query("SELECT DISTINCT CASE WHEN m.senderUsername = :username THEN m.receiverUsername ELSE m.senderUsername END " +
            "FROM Message m WHERE m.senderUsername = :username OR m.receiverUsername = :username")
    List<String> findActiveConversations(@Param("username") String username);
}