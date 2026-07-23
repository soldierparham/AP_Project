package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Rating;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.RatingRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ⭐ امتیازدهی به فروشنده (۱ تا ۵) — مطابق سند پروژه:
 *   POST /api/ratings                 ← ثبت امتیاز { "adId": 1, "score": 5, "comment": "..." } (نظر اختیاری)
 *   GET  /api/ratings/ad/{adId}       ← میانگین امتیاز فروشنده یک آگهی
 *   GET  /api/ratings/seller/{username} ← میانگین امتیاز + لیست نظرات یک فروشنده
 *   GET  /api/ratings/my              ← امتیاز و نظرات دریافتی کاربر جاری (برای صفحه پروفایل)
 * قوانین: امتیاز به خود ممنوع، امتیاز تکراری برای یک آگهی ممنوع
 * 🚫 جدید: امتیازدهی به فروشنده مسدودشده و امتیازدهی توسط کاربر مسدودشده ممنوع است
 */
@RestController
@RequestMapping("/api/ratings")
@CrossOrigin(origins = "*")
public class RatingController {

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private ConversationRepository conversationRepository;

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
     * ثبت امتیاز (و نظر اختیاری) برای فروشنده یک آگهی؛ شامل بررسی قوانین (عدم امتیاز به خود، عدم تکرار، داشتن گفتگو).
     *
     * @param body بدنه درخواست
     * @param principal پارامتر principal
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @Transactional
    @PostMapping
    public ResponseEntity submitRating(@RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        if (body == null || body.get("adId") == null || body.get("score") == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "فیلدهای adId و score الزامی هستند.", "status", 400));
        }

        long adId;
        int score;
        try {
            adId = Long.parseLong(body.get("adId").toString());
            score = Integer.parseInt(body.get("score").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "مقادیر ارسالی نامعتبر هستند.", "status", 400));
        }

        if (score < 1 || score > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "امتیاز باید عددی بین ۱ تا ۵ باشد.", "status", 400));
        }

        Optional adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        Advertisement ad = (Advertisement) adOpt.get();
        String rater = principal.getName();
        String seller = ad.getOwnerUsername();

        if (rater.equals(seller)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شما نمی‌توانید به خودتان امتیاز دهید.", "status", 400));
        }

        // 🚫 جدید: کاربر مسدودشده اجازه امتیازدهی ندارد
        if (isBlocked(rater)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "حساب کاربری شما مسدود شده است و امکان امتیازدهی ندارید.", "status", 403));
        }

        // 🚫 جدید: امتیازدهی به فروشنده مسدودشده ممنوع است
        if (isBlocked(seller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "این فروشنده توسط مدیر مسدود شده است و امکان امتیازدهی به او وجود ندارد.", "status", 403));
        }

        // ✅ فقط خریداری که درباره این کالا با فروشنده گفتگو کرده (یا از طریق چت خرید را انجام داده) می‌تواند امتیاز دهد
        boolean hasConversation = conversationRepository
                .findByAdvertisementIdAndBuyerUsernameAndSellerUsername(adId, rater, seller)
                .isPresent();
        if (!hasConversation) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "فقط در صورتی می‌توانید به فروشنده امتیاز دهید که درباره این کالا با او گفتگو یا خرید کرده باشید.", "status", 403));
        }

        if (ratingRepository.existsByRaterUsernameAndAdvertisementId(rater, adId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شما قبلاً برای این آگهی امتیاز ثبت کرده‌اید.", "status", 400));
        }

        // 💬 نظر اختیاری — اگر خالی باشد ذخیره نمی‌شود
        String comment = body.get("comment") == null ? null : body.get("comment").toString().trim();
        if (comment != null && comment.isEmpty()) comment = null;
        if (comment != null && comment.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "متن نظر حداکثر می‌تواند ۱۰۰۰ کاراکتر باشد.", "status", 400));
        }

        Rating rating = new Rating(rater, seller, ad, score);
        rating.setComment(comment);
        ratingRepository.save(rating);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", comment != null ? "امتیاز و نظر شما با موفقیت ثبت شد." : "امتیاز شما با موفقیت ثبت شد.",
                "average", averageForSeller(seller),
                "count", ratingRepository.findBySellerUsername(seller).size()
        ));
    }

    /**
     * مقدار «rating for ad» را برمی‌گرداند.
     *
     * @param adId شناسه آگهی
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @GetMapping("/ad/{adId}")
    public ResponseEntity getRatingForAd(@PathVariable Long adId) {
        Optional adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        String seller = ((Advertisement) adOpt.get()).getOwnerUsername();
        List ratings = ratingRepository.findBySellerUsername(seller);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "sellerUsername", seller,
                "average", averageForSeller(seller),
                "count", ratings.size()
        ));
    }

    /**
     * مقدار «rating for seller» را برمی‌گرداند.
     *
     * @param username نام کاربری
     * @return پاسخ HTTP شامل وضعیت و بدنه نتیجه عملیات
     */
    @GetMapping("/seller/{username}")
    public ResponseEntity getRatingForSeller(@PathVariable String username) {
        return buildSellerRatingsResponse(username);
    }

    /**
     * 👤 امتیاز و نظرات دریافتی کاربر جاری به‌عنوان فروشنده — برای نمایش در صفحه پروفایل
     */
    @GetMapping("/my")
    public ResponseEntity getMyRatings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }
        return buildSellerRatingsResponse(principal.getName());
    }

    /**
     * 📦 پاسخ مشترک: میانگین، تعداد و لیست نظرات یک فروشنده (جدیدترین اول)
     */
    private ResponseEntity buildSellerRatingsResponse(String username) {
        List<Rating> ratings = new ArrayList<>(ratingRepository.findBySellerUsername(username));
        ratings.sort(Comparator.comparing(Rating::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Rating r : ratings) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rater", r.getRaterUsername());
            item.put("score", r.getScore());
            item.put("comment", r.getComment() == null ? "" : r.getComment());
            item.put("adTitle", (r.getAdvertisement() != null && r.getAdvertisement().getTitle() != null)
                    ? r.getAdvertisement().getTitle() : "");
            item.put("createdAt", r.getCreatedAt() == null ? "" : r.getCreatedAt().toString());
            reviews.add(item);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "sellerUsername", username,
                "average", averageForSeller(username),
                "count", ratings.size(),
                "reviews", reviews
        ));
    }

    /**
     * متد «averageForSeller»؛ بخشی از عملکرد کلاس RatingController را پیاده‌سازی می‌کند.
     *
     * @param sellerUsername نام کاربری فروشنده
     * @return مقدار عددی نتیجه
     */
    private double averageForSeller(String sellerUsername) {
        List<Rating> ratings = ratingRepository.findBySellerUsername(sellerUsername);
        double avg = ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
        // گرد کردن تا یک رقم اعشار
        return Math.round(avg * 10.0) / 10.0;
    }
}
