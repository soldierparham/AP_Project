package com.example.backend.config; // 📂 نام پکیج خود را مطمئن شوید

import com.example.backend.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableScheduling
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ۱. تنظیم کامل CORS و غیرفعال کردن CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // ۲. تنظیم دسترسی‌های دقیق بر اساس کنترلر
                .authorizeHttpRequests(auth -> auth
                        // 🔓 آزاد کردن درخواست‌های Preflight (OPTIONS) جهت هماهنگی مرورگر و فرانت‌انند
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // مسیرهای عمومی پروژه شما که نیاز به توکن ندارند
                        .requestMatchers("/api/login", "/api/register", "/api/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 🔓 🚀 اضافه شد: آزاد کردن مسیر آپلود و پوشه دانلود تصاویر
                        .requestMatchers("/api/upload", "/api/upload/**").permitAll() // 🔓 آزاد کردن مسیر با پیشوند api
                        .requestMatchers("/uploads/**").permitAll()

                        // 🔒 بقیه اندپوینت‌ها قفل باشند و نیاز به توکن معتبر دارند
                        .anyRequest().authenticated()
                )

                // ۳. شاه‌کلید رفع ارور ۴۰۳ و تبدیل به ۴۰۱
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("🚨 [SecurityConfig] توکن منقضی یا مفقود شده؛ هدایت کاربر با وضعیت 401. مسیر درخواست: " + request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"نشست شما منقضی شده است. لطفاً دوباره ورود کنید.\"}");
                        })
                )

                // ۴. مدیریت سشن به صورت Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ۵. قرار دادن فیلتر هوشمند قبل از فیلتر اصلی اسپرینگ
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // 🌐 تنظیمات سراسری CORS برای باز کردن کانال ارتباطی فرانت‌انند
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}