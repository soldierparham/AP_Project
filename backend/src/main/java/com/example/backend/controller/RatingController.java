package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Rating;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ⭐ امتیازدهی به فروشنده (۱ تا ۵) — مطابق سند پروژه:
 * POST /api/ratings                    ← ثبت امتیاز { "adId": 1, "score": 5 }
 * GET  /api/ratings/ad/{adId}          ← میانگین امتیاز فروشنده یک آگهی
 * GET  /api/ratings/seller/{username}  ← میانگین امتیاز یک فروشنده
 * قوانین: امتیاز به خود ممنوع، امتیاز تکراری برای یک آگهی ممنوع
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

    @Transactional
    @PostMapping
    public ResponseEntity<?> submitRating(@RequestBody Map<String, Object> body, Principal principal) {
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

        Optional<Advertisement> adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        Advertisement ad = adOpt.get();
        String rater = principal.getName();
        String seller = ad.getOwnerUsername();

        if (rater.equals(seller)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شما نمی‌توانید به خودتان امتیاز دهید.", "status", 400));
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

        ratingRepository.save(new Rating(rater, seller, ad, score));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "امتیاز شما با موفقیت ثبت شد.",
                "average", averageForSeller(seller),
                "count", ratingRepository.findBySellerUsername(seller).size()
        ));
    }

    @GetMapping("/ad/{adId}")
    public ResponseEntity<?> getRatingForAd(@PathVariable Long adId) {
        Optional<Advertisement> adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        String seller = adOpt.get().getOwnerUsername();
        List<Rating> ratings = ratingRepository.findBySellerUsername(seller);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "sellerUsername", seller,
                "average", averageForSeller(seller),
                "count", ratings.size()
        ));
    }

    @GetMapping("/seller/{username}")
    public ResponseEntity<?> getRatingForSeller(@PathVariable String username) {
        List<Rating> ratings = ratingRepository.findBySellerUsername(username);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "sellerUsername", username,
                "average", averageForSeller(username),
                "count", ratings.size()
        ));
    }

    private double averageForSeller(String sellerUsername) {
        List<Rating> ratings = ratingRepository.findBySellerUsername(sellerUsername);
        double avg = ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);
        // گرد کردن تا یک رقم اعشار
        return Math.round(avg * 10.0) / 10.0;
    }
}
