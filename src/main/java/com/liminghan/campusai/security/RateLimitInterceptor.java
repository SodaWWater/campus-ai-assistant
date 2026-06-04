package com.liminghan.campusai.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory sliding-window rate limiter.
 * <p>
 * Defaults: 100 req/min for general endpoints, 10 req/min for /api/chat/ask.
 * Responses include {@code X-RateLimit-Remaining} headers.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final boolean enabled;
    private final int generalRpm;
    private final int chatRpm;
    private final Map<String, AtomicInteger> generalCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> chatCounters = new ConcurrentHashMap<>();

    public RateLimitInterceptor(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.general-rpm:100}") int generalRpm,
            @Value("${app.rate-limit.chat-rpm:10}") int chatRpm) {
        this.enabled = enabled;
        this.generalRpm = generalRpm;
        this.chatRpm = chatRpm;

        // Reset counters every minute
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-reset");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            generalCounters.clear();
            chatCounters.clear();
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!enabled) {
            return true;
        }

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        boolean isChatEndpoint = path.startsWith("/api/chat/ask");
        Map<String, AtomicInteger> counters = isChatEndpoint ? chatCounters : generalCounters;
        int limit = isChatEndpoint ? chatRpm : generalRpm;

        AtomicInteger counter = counters.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        int remaining = Math.max(0, limit - current);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (current > limit) {
            log.warn("Rate limit exceeded: ip={}, path={}, count={}", clientIp, path, current);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}
