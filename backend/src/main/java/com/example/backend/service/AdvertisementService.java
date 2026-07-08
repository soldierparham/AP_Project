package com.example.backend.service;

import com.example.backend.model.Advertisement;
import com.example.backend.repository.AdvertisementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    // ۱. دریافت همه آگهی‌های موجود در سیستم
    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    // ۲. دریافت آگهی‌های اختصاصی یک کاربر
    public List<Advertisement> getAdsByUsername(String username) {
        return advertisementRepository.findByOwnerUsername(username);
    }

    // ۳. پیدا کردن یک آگهی بر اساس شناسه (ID)
    public Optional<Advertisement> getAdById(Long id) {
        return advertisementRepository.findById(id);
    }

    // ۴. ثبت آگهی جدید یا به‌روزرسانی کامل
    public Advertisement saveAdvertisement(Map<String, Object> data, String username) {
        Advertisement ad = new Advertisement();
        ad.setTitle((String) data.get("title"));
        ad.setDescription((String) data.get("description"));
        // تبدیل امن قیمت به Double
        ad.setPrice(Double.valueOf(data.get("price").toString()));
        ad.setOwnerUsername(username); // ست کردن مالک آگهی

        return advertisementRepository.save(ad);
    }

    // ۵. ویرایش آگهی موجود
    public Advertisement updateAdvertisement(Advertisement existingAd, Map<String, Object> updatedData) {
        if (updatedData.containsKey("title")) existingAd.setTitle((String) updatedData.get("title"));
        if (updatedData.containsKey("description")) existingAd.setDescription((String) updatedData.get("description"));
        if (updatedData.containsKey("price")) existingAd.setPrice(Double.valueOf(updatedData.get("price").toString()));

        return advertisementRepository.save(existingAd);
    }

    // ۶. حذف آگهی از دیتابیس
    public void deleteAd(Long id) {
        advertisementRepository.deleteById(id);
    }
}