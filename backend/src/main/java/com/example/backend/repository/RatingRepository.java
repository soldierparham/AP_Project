package com.example.backend.repository;

import com.example.backend.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ریپازیتوری JPA امتیازها؛ کوئری‌های یافتن امتیازهای فروشنده و بررسی تکراری نبودن امتیاز.
 */
public interface RatingRepository extends JpaRepository<Rating, Long> {

    /**
     * «by seller username» را جستجو و پیدا می‌کند.
     *
     * @param sellerUsername نام کاربری فروشنده
     * @return لیست نتایج
     */
    List<Rating> findBySellerUsername(String sellerUsername);

    /**
     * بررسی وجود «by rater username and advertisement id».
     *
     * @param raterUsername پارامتر raterUsername
     * @param advertisementId پارامتر advertisementId
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByRaterUsernameAndAdvertisementId(String raterUsername, Long advertisementId);

    // پاکسازی امتیازها هنگام حذف آگهی
    /**
     * «by advertisement id» را حذف می‌کند.
     *
     * @param advertisementId پارامتر advertisementId
     */
    void deleteByAdvertisementId(Long advertisementId);
}
