package com.example.backend.repository;

import com.example.backend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ریپازیتوری JPA مکالمه‌ها؛ کوئری‌های یافتن گفتگو بین کاربران.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // پیدا کردن گفتگوی خاص بین خریدار، فروشنده و آگهی (برای جلوگیری از تکراری شدن)
    Optional<Conversation> findByAdvertisementIdAndBuyerUsernameAndSellerUsername(
            Long advertisementId, String buyerUsername, String sellerUsername);

    // واکشی تمام گفتگوهای یک کاربر (چه به عنوان خریدار چه فروشنده)
    /**
     * «by buyer username or seller username» را جستجو و پیدا می‌کند.
     *
     * @param buyerUsername پارامتر buyerUsername
     * @param sellerUsername نام کاربری فروشنده
     * @return لیست نتایج
     */
    List<Conversation> findByBuyerUsernameOrSellerUsername(String buyerUsername, String sellerUsername);

    // 🔧 جدید: پیدا کردن همه مکالمات یک آگهی (برای حذف cascade هنگام حذف آگهی)
    /**
     * «by advertisement id» را جستجو و پیدا می‌کند.
     *
     * @param advertisementId پارامتر advertisementId
     * @return لیست نتایج
     */
    List<Conversation> findByAdvertisementId(Long advertisementId);
}
