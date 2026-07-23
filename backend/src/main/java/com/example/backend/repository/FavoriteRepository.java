package com.example.backend.repository;

import com.example.backend.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ریپازیتوری JPA علاقه‌مندی‌ها؛ کوئری‌های مدیریت علاقه‌مندی کاربران.
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * «by username order by created at desc» را جستجو و پیدا می‌کند.
     *
     * @param username نام کاربری
     * @return لیست نتایج
     */
    List<Favorite> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * «by username and advertisement id» را جستجو و پیدا می‌کند.
     *
     * @param username نام کاربری
     * @param advertisementId پارامتر advertisementId
     * @return مقدار موردنظر در صورت وجود
     */
    Optional<Favorite> findByUsernameAndAdvertisementId(String username, Long advertisementId);

    /**
     * بررسی وجود «by username and advertisement id».
     *
     * @param username نام کاربری
     * @param advertisementId پارامتر advertisementId
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByUsernameAndAdvertisementId(String username, Long advertisementId);

    // پاکسازی علاقه‌مندی‌ها هنگام حذف آگهی
    /**
     * «by advertisement id» را حذف می‌کند.
     *
     * @param advertisementId پارامتر advertisementId
     */
    void deleteByAdvertisementId(Long advertisementId);
}
