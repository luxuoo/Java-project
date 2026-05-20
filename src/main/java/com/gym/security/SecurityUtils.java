package com.gym.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 SecurityContext 中提取当前登录用户信息的工具类。
 * <p>
 * 之所以集中放在这里，是为了避免在每个 Controller 中重复
 * 解析 JWT —— 解析工作已经由 {@link JwtAuthFilter} 完成，
 * 这里只是把结果取出来。
 */
public final class SecurityUtils {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_COACH = "COACH";
    public static final String ROLE_MEMBER = "MEMBER";

    private SecurityUtils() {}

    /** 取当前登录用户名（来自 JWT subject）。未登录返回 null。 */
    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object p = auth.getPrincipal();
        return p == null ? null : p.toString();
    }

    /** 取当前登录用户角色（ADMIN/COACH/MEMBER）。未登录返回 null。 */
    public static String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return null;
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(s -> s != null && s.startsWith("ROLE_"))
                .map(s -> s.substring(5))
                .findFirst().orElse(null);
    }

    public static boolean isAdmin() { return ROLE_ADMIN.equals(currentRole()); }
    public static boolean isCoach() { return ROLE_COACH.equals(currentRole()); }
    public static boolean isMember() { return ROLE_MEMBER.equals(currentRole()); }
}
