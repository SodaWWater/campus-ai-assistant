package com.liminghan.campusai.security;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Centralized utility for extracting current-user information from the
 * Spring Security context.  All controllers and services should use this
 * instead of duplicating {@code getCurrentUserId()} helpers.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Returns the database id of the currently authenticated user.
     *
     * @throws BusinessException(FORBIDDEN) if no authentication is present
     *         or the principal cannot be resolved to a user id.
     */
    public static Long getCurrentUserId() {
        Authentication auth = requireAuthentication();

        // Prefer CampusUserDetails (set by JwtAuthenticationFilter after Phase-3)
        if (auth.getPrincipal() instanceof CampusUserDetails userDetails) {
            return userDetails.getId();
        }

        // Backward-compatible path: read userId from JWT Claims stored in details
        if (auth.getDetails() instanceof Claims claims) {
            Long userId = claims.get("userId", Long.class);
            if (userId != null) {
                return userId;
            }
        }

        throw new BusinessException(ErrorCode.FORBIDDEN, "无法获取当前用户ID");
    }

    public static String getCurrentUsername() {
        Authentication auth = requireAuthentication();
        if (auth.getPrincipal() instanceof CampusUserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth.getName();
    }

    public static String getCurrentUserRole() {
        Authentication auth = requireAuthentication();
        return auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无角色信息"));
    }

    public static boolean isCurrentUserAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }

    private static Authentication requireAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return auth;
    }
}
