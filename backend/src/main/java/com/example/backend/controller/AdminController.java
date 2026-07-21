package com.example.backend.controller;

import com.example.backend.model.Advertisement;
import com.example.backend.model.Category;
import com.example.backend.model.City;
import com.example.backend.model.User;
import com.example.backend.repository.*;
import com.example.backend.service.AdvertisementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🛡️ پنل مدیریت (فقط برای کاربران با نقش ADMIN) — مطابق سند پروژه:
 * - بررسی و تایید/رد/حذف آگهی‌ها
 * - مدیریت کاربران (مسدود/رفع مسدودی)
 * - مدیریت دسته‌بندی‌ها (🚫 جدید: با حذف دسته‌بندی، همه آگهی‌های آن دسته نیز حذف می‌شوند)
 * - داشبورد آماری (امتیازی)
 * نکته امنیتی: نقش کاربر از دیتابیس خوانده می‌شود نه از توکن، تا تغییر نقش فوراً اعمال شود.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private AdvertisementService advertisementService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private RatingRepository ratingRepository;

    // ---------- ابزار کمکی: کنترل دسترسی مدیر ----------

    private ResponseEntity checkAdminAccess(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "ابتدا وارد حساب کاربری خود شوید.", "status", 401));
        }
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty() || !"ADMIN".equalsIgnoreCase(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "دسترسی فقط برای مدیر سیستم مجاز است.", "status", 403));
        }
        return null;
    }

    // ---------- مدیریت آگهی‌ها ----------

    /**
     * لیست آگهی‌ها بر اساس وضعیت (پیش‌فرض: PENDING ، مقدار ALL = همه)
     */
    @GetMapping("/advertisements")
    public ResponseEntity listAdvertisements(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            Principal principal) {

        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        List<Advertisement> ads = "ALL".equalsIgnoreCase(status)
                ? advertisementRepository.findAll()
                : advertisementRepository.findByStatus(status.toUpperCase());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", ads
        ));
    }

    /**
     * ✅ تایید آگهی ← وضعیت ACTIVE (نمایش عمومی)
     */
    @Transactional
    @PostMapping("/advertisements/{id}/approve")
    public ResponseEntity approveAdvertisement(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<Advertisement> adOpt = advertisementRepository.findById(id);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        Advertisement ad = adOpt.get();
        ad.setStatus("ACTIVE");
        ad.setAdminNote(null);
        advertisementRepository.save(ad);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "آگهی تایید و منتشر شد.",
                "data", ad
        ));
    }

    /**
     * ❌ رد آگهی با یادداشت اختیاری مدیر ← وضعیت REJECTED
     */
    @Transactional
    @PostMapping("/advertisements/{id}/reject")
    public ResponseEntity rejectAdvertisement(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            Principal principal) {

        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<Advertisement> adOpt = advertisementRepository.findById(id);
        if (adOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        String note = null;
        if (body != null && body.get("note") != null && !body.get("note").toString().isBlank()) {
            note = body.get("note").toString();
        }

        Advertisement ad = adOpt.get();
        ad.setStatus("REJECTED");
        ad.setAdminNote(note);
        advertisementRepository.save(ad);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "آگهی رد شد.",
                "data", ad
        ));
    }

    /**
     * 🗑️ حذف آگهی توسط مدیر (مثلاً آگهی نامناسب)
     */
    @Transactional
    @DeleteMapping("/advertisements/{id}")
    public ResponseEntity deleteAdvertisement(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        if (advertisementRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "آگهی مورد نظر یافت نشد.", "status", 404));
        }

        advertisementService.deleteAd(id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "آگهی توسط مدیر حذف شد."
        ));
    }

    // ---------- مدیریت کاربران ----------

    @GetMapping("/users")
    public ResponseEntity listUsers(Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName() == null ? "" : u.getName());
            m.put("username", u.getUsername());
            m.put("phoneNumber", u.getPhoneNumber() == null ? "" : u.getPhoneNumber());
            m.put("email", u.getEmail() == null ? "" : u.getEmail());
            m.put("role", u.getRole() == null ? "USER" : u.getRole());
            m.put("status", u.getStatus() == null ? "ACTIVE" : u.getStatus());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", users
        ));
    }

    /**
     * 🚫 مسدود کردن کاربر (توکن فعلی او هم باطل می‌شود)
     */
    @Transactional
    @PostMapping("/users/{id}/block")
    public ResponseEntity blockUser(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "کاربر مورد نظر یافت نشد.", "status", 404));
        }

        User user = userOpt.get();
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "امکان مسدود کردن مدیر سیستم وجود ندارد.", "status", 400));
        }

        user.setStatus("BLOCKED");
        user.setToken(null); // باطل کردن نشست فعلی کاربر
        userRepository.saveAndFlush(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "کاربر مسدود شد."
        ));
    }

    @Transactional
    @PostMapping("/users/{id}/unblock")
    public ResponseEntity unblockUser(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "کاربر مورد نظر یافت نشد.", "status", 404));
        }

        User user = userOpt.get();
        user.setStatus("ACTIVE");
        userRepository.saveAndFlush(user);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "مسدودیت کاربر رفع شد."
        ));
    }

    // ---------- مدیریت دسته‌بندی‌ها ----------

    @GetMapping("/categories")
    public ResponseEntity listCategories(Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", categoryRepository.findAll()
        ));
    }

    @Transactional
    @PostMapping("/categories")
    public ResponseEntity addCategory(@RequestBody Map<String, Object> body, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        if (body == null || body.get("name") == null || body.get("name").toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "نام دسته‌بندی الزامی است.", "status", 400));
        }

        String name = body.get("name").toString().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "این دسته‌بندی قبلاً ثبت شده است.", "status", 400));
        }

        Category saved = categoryRepository.save(new Category(name));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "دسته‌بندی جدید اضافه شد.",
                "data", saved
        ));
    }

    /**
     * 🗑️ حذف دسته‌بندی
     * 🚫 جدید: همه آگهی‌های این دسته‌بندی نیز به همراه وابستگی‌ها (گفتگوها، پیام‌ها، علاقه‌مندی‌ها، امتیازها) حذف می‌شوند
     */
    @Transactional
    @DeleteMapping("/categories/{id}")
    public ResponseEntity deleteCategory(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "دسته‌بندی مورد نظر یافت نشد.", "status", 404));
        }

        String categoryName = catOpt.get().getName();

        // 🗑️ حذف تمام آگهی‌های این دسته‌بندی (با پاکسازی کامل وابستگی‌ها از طریق سرویس)
        List<Advertisement> adsInCategory = advertisementRepository.findAll().stream()
                .filter(a -> a.getCategory() != null && a.getCategory().equalsIgnoreCase(categoryName))
                .collect(Collectors.toList());
        for (Advertisement ad : adsInCategory) {
            advertisementService.deleteAd(ad.getId());
        }

        categoryRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "دسته‌بندی و " + adsInCategory.size() + " آگهی مرتبط با آن حذف شد."
        ));
    }

    /**
     * ✏️ جدید: ویرایش (تغییر نام) دسته‌بندی
     * نام دسته در همه آگهی‌های مرتبط نیز به‌روزرسانی می‌شود تا فیلترها درست بمانند.
     */
    @Transactional
    @PutMapping("/categories/{id}")
    public ResponseEntity renameCategory(@PathVariable Long id,
                                         @RequestBody Map<String, Object> body,
                                         Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        if (body == null || body.get("name") == null || body.get("name").toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "نام جدید دسته‌بندی الزامی است.", "status", 400));
        }

        Optional<Category> catOpt = categoryRepository.findById(id);
        if (catOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "دسته‌بندی مورد نظر یافت نشد.", "status", 404));
        }

        String newName = body.get("name").toString().trim();
        Category category = catOpt.get();
        String oldName = category.getName();

        if (!newName.equalsIgnoreCase(oldName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "دسته‌بندی دیگری با این نام وجود دارد.", "status", 400));
        }

        category.setName(newName);
        categoryRepository.save(category);

        // به‌روزرسانی نام دسته در همه آگهی‌های مرتبط
        List<Advertisement> ads = advertisementRepository.findByCategory(oldName);
        for (Advertisement ad : ads) {
            ad.setCategory(newName);
        }
        advertisementRepository.saveAll(ads);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "دسته‌بندی ویرایش شد و " + ads.size() + " آگهی مرتبط به‌روزرسانی گردید.",
                "data", category
        ));
    }

    // ---------- مدیریت شهرها ----------

    @GetMapping("/cities")
    public ResponseEntity listCities(Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", cityRepository.findAll()
        ));
    }

    @Transactional
    @PostMapping("/cities")
    public ResponseEntity addCity(@RequestBody Map<String, Object> body, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        if (body == null || body.get("name") == null || body.get("name").toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "نام شهر الزامی است.", "status", 400));
        }

        String name = body.get("name").toString().trim();
        if (cityRepository.existsByNameIgnoreCase(name)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "این شهر قبلاً ثبت شده است.", "status", 400));
        }

        City saved = cityRepository.save(new City(name));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "شهر جدید اضافه شد.",
                "data", saved
        ));
    }

    /**
     * ✏️ جدید: ویرایش (تغییر نام) شهر
     * نام شهر در همه آگهی‌های مرتبط نیز به‌روزرسانی می‌شود تا فیلتر شهر درست بماند.
     */
    @Transactional
    @PutMapping("/cities/{id}")
    public ResponseEntity renameCity(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body,
                                     Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        if (body == null || body.get("name") == null || body.get("name").toString().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "نام جدید شهر الزامی است.", "status", 400));
        }

        Optional<City> cityOpt = cityRepository.findById(id);
        if (cityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "شهر مورد نظر یافت نشد.", "status", 404));
        }

        String newName = body.get("name").toString().trim();
        City city = cityOpt.get();
        String oldName = city.getName();

        if (!newName.equalsIgnoreCase(oldName) && cityRepository.existsByNameIgnoreCase(newName)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "شهر دیگری با این نام وجود دارد.", "status", 400));
        }

        city.setName(newName);
        cityRepository.save(city);

        // به‌روزرسانی نام شهر در همه آگهی‌های مرتبط
        List<Advertisement> ads = advertisementRepository.findByCity(oldName);
        for (Advertisement ad : ads) {
            ad.setCity(newName);
        }
        advertisementRepository.saveAll(ads);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "شهر ویرایش شد و " + ads.size() + " آگهی مرتبط به‌روزرسانی گردید.",
                "data", city
        ));
    }

    /**
     * حذف شهر از لیست شهرها
     * همه آگهی‌های ثبت‌شده در آن شهر نیز با پاکسازی کامل وابستگی‌ها (چت‌ها، تصاویر، علاقه‌مندی‌ها) حذف می‌شوند.
     */
    @Transactional
    @DeleteMapping("/cities/{id}")
    public ResponseEntity deleteCity(@PathVariable Long id, Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        Optional<City> cityOpt = cityRepository.findById(id);
        if (cityOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "شهر مورد نظر یافت نشد.", "status", 404));
        }

        String name = cityOpt.get().getName();

        // حذف تمام آگهی‌های این شهر (با پاکسازی کامل وابستگی‌ها از طریق سرویس)
        List<Advertisement> adsInCity = advertisementRepository.findByCity(name);
        for (Advertisement ad : adsInCity) {
            advertisementService.deleteAd(ad.getId());
        }

        cityRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "شهر «" + name + "» و " + adsInCity.size() + " آگهی مرتبط با آن حذف شد."
        ));
    }

    // ---------- داشبورد آماری (امتیازی) ----------

    @GetMapping("/stats")
    public ResponseEntity getStats(Principal principal) {
        ResponseEntity denied = checkAdminAccess(principal);
        if (denied != null) return denied;

        long blockedUsers = userRepository.findAll().stream()
                .filter(u -> "BLOCKED".equalsIgnoreCase(u.getStatus()))
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("blockedUsers", blockedUsers);
        stats.put("totalAds", advertisementRepository.count());
        stats.put("pendingAds", advertisementRepository.countByStatus("PENDING"));
        stats.put("activeAds", advertisementRepository.countByStatus("ACTIVE"));
        stats.put("rejectedAds", advertisementRepository.countByStatus("REJECTED"));
        stats.put("soldAds", advertisementRepository.countByStatus("SOLD"));
        stats.put("totalConversations", conversationRepository.count());
        stats.put("totalMessages", messageRepository.count());
        stats.put("totalRatings", ratingRepository.count());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", stats
        ));
    }
}
