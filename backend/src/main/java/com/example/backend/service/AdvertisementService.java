package com.example.backend.service;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Conversation;
import com.example.backend.repository.AdvertisementRepository;
import com.example.backend.repository.ConversationRepository;
import com.example.backend.repository.MessageRepository;
import com.example.backend.repository.FavoriteRepository;
import com.example.backend.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RatingRepository ratingRepository;

    // دریافت تمام آگهی‌ها
    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    // دریافت آگهی‌های کاربران دیگر (به جز کاربر جاری) — بدون فیلتر وضعیت (نسخه قدیمی)
    public List<Advertisement> getAdsExceptOwner(String ownerUsername) {
        return advertisementRepository.findByOwnerUsernameNot(ownerUsername);
    }

    /**
     * 🔍 جدید: دریافت آگهی‌های «فعال» دیگران + جستجو، فیلتر ترکیبی و مرتب‌سازی (مطابق سند پروژه)
     * - فقط آگهی‌هایی با وضعیت ACTIVE برای عموم قابل مشاهده هستند
     * - search: جستجوی کلیدواژه در عنوان و توضیحات
     * - category / city / minPrice / maxPrice: فیلترهای ترکیبی
     * - sort: newest (جدیدترین) / cheapest (ارزان‌ترین) / expensive (گران‌ترین)
     */
    public List<Advertisement> getActiveAdsExceptOwner(String username,
                                                       String search,
                                                       String category,
                                                       String city,
                                                       Long minPrice,
                                                       Long maxPrice,
                                                       String sort) {
        List<Advertisement> ads = advertisementRepository.findByStatusAndOwnerUsernameNot("ACTIVE", username);
        Stream<Advertisement> stream = ads.stream();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            stream = stream.filter(a ->
                    (a.getTitle() != null && a.getTitle().toLowerCase().contains(q)) ||
                    (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)));
        }
        if (category != null && !category.isBlank()) {
            stream = stream.filter(a -> category.equalsIgnoreCase(a.getCategory()));
        }
        if (city != null && !city.isBlank()) {
            stream = stream.filter(a -> city.equalsIgnoreCase(a.getCity()));
        }
        if (minPrice != null) {
            stream = stream.filter(a -> a.getPrice() != null && a.getPrice() >= minPrice);
        }
        if (maxPrice != null) {
            stream = stream.filter(a -> a.getPrice() != null && a.getPrice() <= maxPrice);
        }

        Comparator<Advertisement> comparator;
        String sortKey = (sort == null || sort.isBlank()) ? "newest" : sort;
        switch (sortKey) {
            case "cheapest":
                comparator = Comparator.comparing(Advertisement::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "expensive":
                comparator = Comparator.comparing(Advertisement::getPrice,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
                break;
            default: // newest
                comparator = Comparator.comparing(Advertisement::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        return stream.sorted(comparator).collect(Collectors.toList());
    }

    // دریافت آگهی‌های اختصاصی یک کاربر (با هر وضعیتی، تا مالک وضعیت آگهی خود را ببیند)
    public List<Advertisement> getAdsByUsername(String username) {
        return advertisementRepository.findByOwnerUsername(username);
    }

    /**
     * 🔍 جدید: آگهی‌های خود کاربر + جستجو، فیلتر ترکیبی و مرتب‌سازی
     * (برای اعمال فیلترهای ردیف بالا در صفحه «آگهی‌های من» بدون بازگشت به صفحه اصلی)
     */
    public List<Advertisement> getAdsByUsername(String username,
                                                String search,
                                                String category,
                                                String city,
                                                Long minPrice,
                                                Long maxPrice,
                                                String sort) {
        Stream<Advertisement> stream = advertisementRepository.findByOwnerUsername(username).stream();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            stream = stream.filter(a ->
                    (a.getTitle() != null && a.getTitle().toLowerCase().contains(q)) ||
                    (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)));
        }
        if (category != null && !category.isBlank()) {
            stream = stream.filter(a -> category.equalsIgnoreCase(a.getCategory()));
        }
        if (city != null && !city.isBlank()) {
            stream = stream.filter(a -> city.equalsIgnoreCase(a.getCity()));
        }
        if (minPrice != null) {
            stream = stream.filter(a -> a.getPrice() != null && a.getPrice() >= minPrice);
        }
        if (maxPrice != null) {
            stream = stream.filter(a -> a.getPrice() != null && a.getPrice() <= maxPrice);
        }

        Comparator<Advertisement> comparator;
        String sortKey = (sort == null || sort.isBlank()) ? "newest" : sort;
        switch (sortKey) {
            case "cheapest":
                comparator = Comparator.comparing(Advertisement::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "expensive":
                comparator = Comparator.comparing(Advertisement::getPrice,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed();
                break;
            default: // newest
                comparator = Comparator.comparing(Advertisement::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
                break;
        }

        return stream.sorted(comparator).collect(Collectors.toList());
    }

    public Optional<Advertisement> getAdById(Long id) {
        return advertisementRepository.findById(id);
    }

    /**
     * 📥 ثبت آگهی جدید — آگهی جدید همیشه با وضعیت PENDING (در انتظار تایید مدیر) ثبت می‌شود
     */
    public Advertisement saveAdvertisement(Map<String, Object> data, String username) {
        Advertisement ad = new Advertisement();
        ad.setTitle((String) data.get("title"));
        ad.setDescription((String) data.get("description"));
        ad.setPrice(Long.parseLong(data.get("price").toString().replaceAll("\\..*", "")));
        ad.setOwnerUsername(username);

        if (data.containsKey("city")) {
            ad.setCity((String) data.get("city"));
        }
        if (data.containsKey("category")) {
            ad.setCategory((String) data.get("category"));
        }

        String imageUrl = null;
        if (data.containsKey("image_url") && data.get("image_url") != null) {
            imageUrl = (String) data.get("image_url");
        } else if (data.containsKey("imageUrl") && data.get("imageUrl") != null) {
            imageUrl = (String) data.get("imageUrl");
        }
        ad.setImageUrl(imageUrl);

        // 🟡 مطابق سند پروژه: آگهی جدید تا تایید مدیر نمایش عمومی ندارد
        ad.setStatus("PENDING");
        ad.setCreatedAt(LocalDateTime.now());

        return advertisementRepository.save(ad);
    }

    /**
     * ✏️ ویرایش آگهی موجود — پس از ویرایش، آگهی برای بازبینی مجدد به وضعیت PENDING برمی‌گردد
     */
    public Advertisement updateAdvertisement(Advertisement existingAd, Map<String, Object> updatedData) {
        if (updatedData.containsKey("title")) {
            existingAd.setTitle((String) updatedData.get("title"));
        }
        if (updatedData.containsKey("description")) {
            existingAd.setDescription((String) updatedData.get("description"));
        }
        if (updatedData.containsKey("price")) {
            existingAd.setPrice(Long.parseLong(updatedData.get("price").toString().replaceAll("\\..*", "")));
        }
        if (updatedData.containsKey("city")) {
            existingAd.setCity((String) updatedData.get("city"));
        }
        if (updatedData.containsKey("category")) {
            existingAd.setCategory((String) updatedData.get("category"));
        }
        if (updatedData.containsKey("image_url") || updatedData.containsKey("imageUrl")) {
            String imageUrl = updatedData.containsKey("image_url")
                    ? (String) updatedData.get("image_url")
                    : (String) updatedData.get("imageUrl");
            existingAd.setImageUrl(imageUrl);
        }

        // 🟡 بازگشت به صف بررسی مدیر پس از ویرایش
        existingAd.setStatus("PENDING");
        existingAd.setAdminNote(null);

        return advertisementRepository.save(existingAd);
    }

    /**
     * ✅ جدید: علامت‌گذاری آگهی به عنوان فروخته‌شده
     */
    public Advertisement markAsSold(Advertisement ad) {
        ad.setStatus("SOLD");
        return advertisementRepository.save(ad);
    }

    /**
     * 🗑️ حذف آگهی + پاکسازی علاقه‌مندی‌ها و امتیازهای مرتبط
     */
    @Transactional
    public void deleteAd(Long id) {
        // 🔧 حذف به ترتیب: ابتدا مکالمات و پیام‌های مرتبط، سپس علاقه‌مندی‌ها و امتیازات، آنگاه خود آگهی
        // (foreign key در Conversation و Message اجازه نمی‌دهد مستقیم آگهی را حذف کنیم بدون پاک‌کردن مکالمات)
        List<Conversation> relatedConversations = conversationRepository.findByAdvertisementId(id);
        for (Conversation conv : relatedConversations) {
            messageRepository.deleteByConversationId(conv.getId());
        }
        conversationRepository.deleteAll(relatedConversations);

        favoriteRepository.deleteByAdvertisementId(id);
        ratingRepository.deleteByAdvertisementId(id);
        advertisementRepository.deleteById(id);
    }
}
