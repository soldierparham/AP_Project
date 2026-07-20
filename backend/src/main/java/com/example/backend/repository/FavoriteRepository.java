package com.example.backend.repository;

import com.example.backend.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<Favorite> findByUsernameAndAdvertisementId(String username, Long advertisementId);

    boolean existsByUsernameAndAdvertisementId(String username, Long advertisementId);

    // پاکسازی علاقه‌مندی‌ها هنگام حذف آگهی
    void deleteByAdvertisementId(Long advertisementId);
}
