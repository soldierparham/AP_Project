package com.example.backend.repository;

import com.example.backend.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findBySellerUsername(String sellerUsername);

    boolean existsByRaterUsernameAndAdvertisementId(String raterUsername, Long advertisementId);

    // پاکسازی امتیازها هنگام حذف آگهی
    void deleteByAdvertisementId(Long advertisementId);
}
