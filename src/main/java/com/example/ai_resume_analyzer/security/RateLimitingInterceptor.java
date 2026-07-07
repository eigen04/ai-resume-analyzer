package com.example.ai_resume_analyzer.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Interceptor that tracks and enforces request limits per user/IP using Redis.
 * Falls open (allows requests) if Redis connection is unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    @Value("${rate.limit.uploads:5}")
    private int uploadLimit;

    @Value("${rate.limit.queries:10}")
    private int queryLimit;

    @Value("${rate.limit.anonymous:20}")
    private int anonymousLimit;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String identifier;
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser");

        if (isAuthenticated) {
            identifier = auth.getName();
        } else {
            identifier = getClientIp(request);
        }

        String limitType;
        int limit;

        if (path.contains("/api/v1/resumes/upload") && "POST".equalsIgnoreCase(method)) {
            limitType = "upload";
            limit = uploadLimit;
        } else if ((path.contains("/ask") || path.contains("/match") || path.contains("/suggestions")) && "POST".equalsIgnoreCase(method)) {
            limitType = "query";
            limit = queryLimit;
        } else if (path.contains("/api/v1/auth/")) {
            limitType = "anonymous";
            limit = anonymousLimit;
        } else {
            return true;
        }

        String minuteKey = String.valueOf(System.currentTimeMillis() / 60000);
        String redisKey = "rate:limit:" + limitType + ":" + identifier + ":" + minuteKey;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey, 1);
            if (currentCount != null) {
                if (currentCount == 1) {
                    redisTemplate.expire(redisKey, Duration.ofSeconds(60));
                }
                if (currentCount > limit) {
                    log.warn("Rate limit exceeded for user/ip={}: limitType={}, limit={}, count={}",
                        identifier, limitType, limit, currentCount);
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":429,\"errorCode\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded. Please try again in a minute.\",\"path\":\"" + path + "\"}");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Redis rate limiter failed (failing open): {}", e.getMessage());
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
