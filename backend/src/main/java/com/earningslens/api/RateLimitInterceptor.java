package com.earningslens.api;

import com.earningslens.config.RateLimitProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abuse and cost protection for POST /api/v1/chat (spec 13, roadmap E4.7). Only chat calls cost money,
 * so only they are limited (WebConfig registers this interceptor for that path).
 *
 * 1. Per IP: Bucket4j token bucket, 10 requests per minute by default (earningslens.rate-limit).
 * 2. Global: at most 300 chat requests per UTC day, counted across all users.
 *
 * Both run before the request body is read, so invalid requests count too (they are cheap, but so is spamming).
 * A refused request throws RateLimitExceededException; GlobalExceptionHandler turns it into a 429.
 * State is in memory, which is right for one app instance (the demo runs one). The per-IP map is cleared if it
 * grows past MAX_TRACKED_IPS, which at worst gives a few addresses a fresh minute.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    static final int MAX_TRACKED_IPS = 10_000;

    private final RateLimitProperties settings;
    private final Clock clock;
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    private LocalDate day;
    private int chatsToday;

    // Two constructors: @Autowired tells Spring to use this one (the other lets tests control the clock).
    @Autowired
    public RateLimitInterceptor(RateLimitProperties settings) {
        this(settings, Clock.systemUTC());
    }

    RateLimitInterceptor(RateLimitProperties settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
        this.day = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;   // CORS preflight (OPTIONS) and anything else that is not a chat call
        }
        checkPerIp(clientIp(request));
        checkDailyCap();
        return true;
    }

    private void checkPerIp(String ip) {
        if (bucketsByIp.size() > MAX_TRACKED_IPS) {
            bucketsByIp.clear();
        }
        Bucket bucket = bucketsByIp.computeIfAbsent(ip, key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
            throw new RateLimitExceededException(RateLimitExceededException.Reason.PER_IP, seconds);
        }
    }

    private Bucket newBucket() {
        int perMinute = settings.requestsPerMinutePerIp();
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)))
                .build();
    }

    private synchronized void checkDailyCap() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (!today.equals(day)) {
            day = today;
            chatsToday = 0;
        }
        if (chatsToday >= settings.dailyChatCap()) {
            long seconds = Duration.between(clock.instant(),
                    today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()).toSeconds();
            throw new RateLimitExceededException(RateLimitExceededException.Reason.DAILY_CAP, Math.max(1, seconds));
        }
        chatsToday++;
    }

    /**
     * The address the request came from. Behind Azure's proxy this is only the real client address once
     * server.forward-headers-strategy is set (Phase 8); X-Forwarded-For is never read directly here,
     * because a client can put anything in it.
     */
    static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null || ip.isBlank() ? "unknown" : ip;
    }

    /** Forgets all counts. For tests that share one Spring context. */
    synchronized void reset() {
        bucketsByIp.clear();
        day = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        chatsToday = 0;
    }
}
