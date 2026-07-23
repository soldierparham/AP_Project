package com.example.backend.repository;

import com.example.backend.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ریپازیتوری JPA شهرها؛ دسترسی به داده‌های جدول شهرها.
 */
public interface CityRepository extends JpaRepository<City, Long> {

    /**
     * «by name ignore case» را جستجو و پیدا می‌کند.
     *
     * @param name نام
     * @return مقدار موردنظر در صورت وجود
     */
    Optional<City> findByNameIgnoreCase(String name);

    /**
     * بررسی وجود «by name ignore case».
     *
     * @param name نام
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByNameIgnoreCase(String name);
}
