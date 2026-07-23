package com.example.backend.repository;

import com.example.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * ریپازیتوری JPA دسته‌بندی‌ها؛ دسترسی به داده‌های جدول دسته‌بندی.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * «by name ignore case» را جستجو و پیدا می‌کند.
     *
     * @param name نام
     * @return مقدار موردنظر در صورت وجود
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * بررسی وجود «by name ignore case».
     *
     * @param name نام
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    boolean existsByNameIgnoreCase(String name);

    /** زیردسته‌های یک دسته والد */
    List<Category> findByParentId(Long parentId);
}
