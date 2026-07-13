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

    // دریافت تمام آگهی‌ها (برای کاربرانی که لاگین نیستند)
    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    // دریافت آگهی‌های کاربران دیگر (به جز کاربر جاری)
    public List<Advertisement> getAdsExceptOwner(String ownerUsername) {
        return advertisementRepository.findByOwnerUsernameNot(ownerUsername);
    }

    // دریافت آگهی‌های اختصاصی یک کاربر
    public List<Advertisement> getAdsByUsername(String username) {
        return advertisementRepository.findByOwnerUsername(username);
    }

    public Optional<Advertisement> getAdById(Long id) {
        return advertisementRepository.findById(id);
    }

    /**
     * 📥 ثبت آگهی جدید به همراه ذخیره‌سازی شهر و دسته‌بندی
     */
    public Advertisement saveAdvertisement(Map<String, Object> data, String username) {
        Advertisement ad = new Advertisement();
        ad.setTitle((String) data.get("title"));
        ad.setDescription((String) data.get("description"));
        ad.setPrice(Double.valueOf(data.get("price").toString()));
        ad.setOwnerUsername(username);

        // 🟢 استخراج و مقداردهی فیلدهای جدید شهر و دسته‌بندی
        if (data.containsKey("city")) {
            ad.setCity((String) data.get("city"));
        }
        if (data.containsKey("category")) {
            ad.setCategory((String) data.get("category"));
        }

        return advertisementRepository.save(ad);
    }

    /**
     * ✏️ ویرایش آگهی موجود به همراه پشتیبانی از تغییر شهر و دسته‌بندی
     */
    public Advertisement updateAdvertisement(Advertisement existingAd, Map<String, Object> updatedData) {
        if (updatedData.containsKey("title")) {
            existingAd.setTitle((String) updatedData.get("title"));
        }
        if (updatedData.containsKey("description")) {
            existingAd.setDescription((String) updatedData.get("description"));
        }
        if (updatedData.containsKey("price")) {
            existingAd.setPrice(Double.valueOf(updatedData.get("price").toString()));
        }

        // 🟢 بررسی و اعمال تغییرات جدید روی شهر و دسته‌بندی (در صورت ارسال از سمت کلاینت)
        if (updatedData.containsKey("city")) {
            existingAd.setCity((String) updatedData.get("city"));
        }
        if (updatedData.containsKey("category")) {
            existingAd.setCategory((String) updatedData.get("category"));
        }

        return advertisementRepository.save(existingAd);
    }

    public void deleteAd(Long id) {
        advertisementRepository.deleteById(id);
    }
}