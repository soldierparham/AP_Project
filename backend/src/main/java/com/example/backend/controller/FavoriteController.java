package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Favorite;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ⭐ مدیریت علاقه‌مندی‌ها (نشان‌ها) — مطابق سند پروژه:
 * GET    /api/favorites          ← لیست آگهی‌های نشان‌شده کاربر
 * POST   /api/favorites/{adId}   ← افزودن به علاقه‌مندی‌ها (بدون تکرار)
 * DELETE /api/favorites/{adId}   ← حذف از علاقه‌مندی‌ها
 */
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @GetMapping
    public ResponseEntity<?> getMyFavorites(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        List<Advertisement> favoriteAds = favoriteRepository
                .findByUsernameOrderByCreatedAtDesc(principal.getName())
                .stream()
                .map(Favorite::getAdvertisement)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "لیست علاقه‌مندی‌ها با موفقیت دریافت شد.",
                "data", favoriteAds
        ));
    }

    @Transactional
    @PostMapping("/{adId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long adId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        String username = principal.getName();

        Optional<Advertisement> adOpt = advertisementRepository.findById(adId);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        Advertisement ad = adOpt.get();

        if (username.equals(ad.getOwnerUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "امکان افزودن آگهی خودتان به علاقه‌مندی‌ها وجود ندارد.", "status", 400));
        }

        if (favoriteRepository.existsByUsernameAndAdvertisementId(username, adId)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "این آگهی قبلاً به علاقه‌مندی‌های شما اضافه شده است.", "status", 400));
        }

        favoriteRepository.save(new Favorite(username, ad));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "آگهی به علاقه‌مندی‌های شما اضافه شد."
        ));
    }

    @Transactional
    @DeleteMapping("/{adId}")
    public ResponseEntity<?> removeFavorite(@PathVariable Long adId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }

        String username = principal.getName();

        Optional<Favorite> favOpt = favoriteRepository.findByUsernameAndAdvertisementId(username, adId);
        if (favOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "این آگهی در لیست علاقه‌مندی‌های شما نیست.", "status", 404));
        }

        favoriteRepository.delete(favOpt.get());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "آگهی از علاقه‌مندی‌های شما حذف شد."
        ));
    }
}
