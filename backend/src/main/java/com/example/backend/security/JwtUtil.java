package com.example.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 🔑 کلید مخفی امن برای امضا
    private final String SECRET_KEY = "YourSuperSecretPurpleAndGoldMarketplaceKey123!";
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * 🎫 صدور توکن همراه با نقش
     */
    public String generateToken(String username, String role) {
        // 🌟 اصلاح اصلی: استفاده از .claim بجای .setClaims برای جلوگیری از حذف سایر فیلدها
        long EXPIRATION_TIME = 1000 * 60 * 20;
        return Jwts.builder()
                .claim("role", role)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 🔍 استخراج نام کاربری از داخل توکن
     */
    public String extractUsername(String token) {
        try {
            return getClaims(token).getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        } catch (Exception e) {
            System.out.println("❌ [JwtUtil] خطای استخراج نام کاربری: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🛡️ بررسی اینکه آیا توکن معتبر است و منقضی نشده؟
     */
    public boolean validateToken(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            if (extractedUsername == null) {
                return false;
            }
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            System.out.println("❌ [JwtUtil] خطای متد validateToken: " + e.getMessage());
            return false;
        }
    }

    /**
     * ⏱️ بررسی وضعیت انقضای زمانی توکن
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            if (expiration == null) {
                System.out.println("⚠️ [JwtUtil] فیلد Expiration در توکن null است!");
                return true;
            }
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            System.out.println("⚠️ [JwtUtil] توکن از نظر زمانی اکسپایر شده است.");
            return true;
        } catch (Exception e) {
            // 🌟 شفاف‌سازی خطا: چاپ ارور واقعی در کنسول به جای خفه کردن آن
            System.out.println("❌ [JwtUtil] خطای پنهان در بررسی انقضا: " + e.getMessage());
            return true;
        }
    }

    /**
     * 📦 پارس کردن و استخراج بدنه توکن (Claims)
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .setAllowedClockSkewSeconds(5) // 🌟 افزایش به ۵ ثانیه برای حل اختلاف‌های میلی‌ثانیه‌ای کلاک سیستم
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}