package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * ابزار ساخت، خواندن و اعتبارسنجی توکن JWT
 * - subject: نام کاربری
 * - claim role: نقش کاربر (USER / ADMIN)
 */
@Component
public class JwtUtil {

    private static final String SECRET = "SecondHandMarketplaceSuperSecretJwtSigningKey1234567890!";
    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24 ساعت

    /**
     * مقدار «signing key» را برمی‌گرداند.
     *
     * @return مقدار بازگشتی
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * تولید توکن JWT برای نام کاربری داده‌شده.
     *
     * @param username نام کاربری
     * @param role پارامتر role
     * @return رشته نتیجه
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role == null ? "USER" : role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * «all claims» را از داده ورودی استخراج می‌کند.
     *
     * @param token توکن JWT
     * @return مقدار بازگشتی
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** در صورت انقضا ExpiredJwtException پرتاب می‌شود */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * «role» را از داده ورودی استخراج می‌کند.
     *
     * @param token توکن JWT
     * @return رشته نتیجه
     */
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role == null ? "USER" : role.toString();
    }

    /**
     * بررسی می‌کند که آیا «token expired» برقرار است یا خیر.
     *
     * @param token توکن JWT
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * اعتبارسنجی توکن JWT و بررسی انقضای آن.
     *
     * @param token توکن JWT
     * @param username نام کاربری
     * @return در صورت برقراری شرط true و در غیر این صورت false
     */
    public boolean validateToken(String token, String username) {
        String tokenUsername = extractUsername(token);
        return tokenUsername != null && tokenUsername.equals(username) && !isTokenExpired(token);
    }
}
