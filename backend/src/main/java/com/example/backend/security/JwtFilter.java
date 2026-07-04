package com.example.backend.security;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.equals("/api/login") || path.equals("/api/register") || path.equals("/api/auth/logout");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
            } catch (ExpiredJwtException e) {
                System.out.println("⚠️ توکن کاربر منقضی شده است. در حال پاکسازی دیتابیس...");

                // 🧼 گام اول: نال کردن توکن منقضی شده در دیتابیس با مدیریت خطای SQLite
                try {
                    String expiredUsername = e.getClaims().getSubject();
                    if (expiredUsername != null) {
                        userRepository.findByUsername(expiredUsername).ifPresent(user -> {
                            user.setToken(null); // نال کردن توکن
                            userRepository.saveAndFlush(user); // اعمال آنی
                            System.out.println("🔒 توکن منقضی شده کاربر " + expiredUsername + " با موفقیت در دیتابیس null شد.");
                        });
                    }
                } catch (Exception dbException) {
                    System.out.println("❌ خطای موقت دیتابیس در فیلتر: " + dbException.getMessage());
                }

                // 🚨 گام دوم: ارسال پاسخ 401 همراه با هدرهای CORS برای جلوگیر از ارور 403 مرورگر
                sendCustomUnauthorizedResponse(response, "نشست شما منقضی شده است. لطفاً دوباره ورود کنید.");
                return;

            } catch (Exception e) {
                System.out.println("❌ توکن به طور کلی نامعتبر است: " + e.getMessage());
                sendCustomUnauthorizedResponse(response, "توکن نامعتبر است. دسترسی رد شد.");
                return;
            }
        }

        // بررسی ولید بودن توکن در دیتابیس (در صورتی که منقضی نشده باشد)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // شرط همخوانی توکن فرانت با دیتابیس و عدم انقضا
                if (token.equals(user.getToken()) && !jwtUtil.isTokenExpired(token)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, null
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 🌐 متد کمکی برای تزریق دستی هدرهای CORS و ارسال پاسخ 401 تمیز به فرانت‌انند
     */
    private void sendCustomUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");

        // 🛡️ تزریق هدرهای CORS به پاسخ‌های دستی فیلتر تا مرورگر آن را بلاک (403) نکند
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Cache-Control");

        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}