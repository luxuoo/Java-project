package com.gym.config;

import com.gym.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 登录与认证
                .requestMatchers("/api/auth/login").permitAll()
                // OpenAPI / Swagger
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // 静态资源（含图标 SVG）
                .requestMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .requestMatchers("/css/**", "/js/**", "/icons/**", "/images/**", "/img/**", "/static/**").permitAll()
                // 预检
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 管理员专属
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 教练专属（教练侧后台）
                .requestMatchers("/api/coach-portal/**").hasAnyRole("ADMIN", "COACH")

                // 其他后台 API：会员管理、教练档案、签到、器材、财务、会员卡、看板
                // 学员端有"我的"接口走 /api/me/**，下面单独放行
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "COACH")
                .requestMatchers("/api/member/**").hasAnyRole("ADMIN", "COACH")
                .requestMatchers("/api/coach/**").hasAnyRole("ADMIN", "COACH", "MEMBER") // 教练列表/排班，会员可看
                .requestMatchers(HttpMethod.GET, "/api/course/**").authenticated() // 课程浏览三种角色都可以
                .requestMatchers("/api/course/**").hasAnyRole("ADMIN", "COACH") // 写操作 ADMIN/COACH
                .requestMatchers("/api/checkin/**").hasAnyRole("ADMIN", "COACH")
                .requestMatchers("/api/equipment/**").hasAnyRole("ADMIN", "COACH", "MEMBER")
                .requestMatchers(HttpMethod.GET, "/api/equipment/**").authenticated()
                .requestMatchers("/api/payment/**").hasRole("ADMIN")
                .requestMatchers("/api/card/**").hasAnyRole("ADMIN", "COACH")
                .requestMatchers("/api/notification/my", "/api/notification/unread-count",
                        "/api/notification/*/read", "/api/notification/read-all").authenticated()
                .requestMatchers("/api/notification/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未登录或Token已过期")));
                })
                .accessDeniedHandler((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write(objectMapper.writeValueAsString(Result.fail(403, "权限不足")));
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 注意：使用 allowedOriginPatterns 而非 allowedOrigins，
        // 这样在以后开启 allowCredentials 也兼容；这里通配 "*"。
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
