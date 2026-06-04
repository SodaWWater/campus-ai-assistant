package com.liminghan.campusai.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final CampusUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CampusUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseClaims(token);
                String username = claims.getSubject();

                // Load full user details from database to verify account status
                CampusUserDetails userDetails;
                try {
                    userDetails = (CampusUserDetails) userDetailsService.loadUserByUsername(username);
                } catch (UsernameNotFoundException e) {
                    log.warn("JWT valid but user not found in database: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                if (!userDetails.isEnabled()) {
                    log.warn("JWT valid but user account is disabled: {}", username);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "账号已被禁用");
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                // Preserve JWT claims in details for backward compatibility
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
