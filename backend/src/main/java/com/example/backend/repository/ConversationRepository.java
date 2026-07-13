package com.example.backend.repository;

import com.example.backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // پیدا کردن گفتگوی خاص بین خریدار و فروشنده روی یک آگهی (برای جلوگیری از ثبت چت تکراری)
    Optional<Conversation> findByAdvertisementIdAndBuyerUsernameAndSellerUsername(Long advertisementId, String buyerUsername, String sellerUsername);

    // واکشی تمام گفتگوهای یک کاربر (چه به عنوان خریدار و چه فروشنده)
    List<Conversation> findByBuyerUsernameOrSellerUsername(String buyerUsername, String sellerUsername);
}