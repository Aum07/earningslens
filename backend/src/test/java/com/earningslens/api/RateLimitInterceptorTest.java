package com.earningslens.api;

import com.earningslens.api.RateLimitExceededException.Reason;
import com.earningslens.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Roadmap E4.7: the per-IP limit and the daily cap, without Spring. A settable clock tests the daily reset. */
class RateLimitInterceptorTest {

    @Test
    void perIpLimitRefusesTheFourthRequestInAMinuteButNotOtherAddresses() {
        RateLimitInterceptor limiter = new RateLimitInterceptor(new RateLimitProperties(3, 100));

        for (int i = 0; i < 3; i++) {
            assertThat(chat(limiter, "10.0.0.1")).isTrue();
        }
        RateLimitExceededException refused =
                catchThrowableOfType(RateLimitExceededException.class, () -> chat(limiter, "10.0.0.1"));
        assertThat(refused.reason()).isEqualTo(Reason.PER_IP);
        assertThat(refused.retryAfterSeconds()).isBetween(1L, 60L);

        assertThat(chat(limiter, "10.0.0.2")).isTrue();   // a different address has its own bucket
    }

    @Test
    void onlyPostIsCounted() {
        RateLimitInterceptor limiter = new RateLimitInterceptor(new RateLimitProperties(2, 100));

        for (int i = 0; i < 5; i++) {
            assertThat(call(limiter, "OPTIONS", "10.0.0.1")).isTrue();   // CORS preflight
            assertThat(call(limiter, "GET", "10.0.0.1")).isTrue();
        }
        assertThat(chat(limiter, "10.0.0.1")).isTrue();
        assertThat(chat(limiter, "10.0.0.1")).isTrue();
        assertThatThrownBy(() -> chat(limiter, "10.0.0.1")).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void dailyCapCountsAllAddressesAndResetsAtMidnightUtc() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-04T23:00:00Z"));
        RateLimitInterceptor limiter = new RateLimitInterceptor(new RateLimitProperties(100, 2), clock);

        assertThat(chat(limiter, "10.0.0.1")).isTrue();
        assertThat(chat(limiter, "10.0.0.2")).isTrue();
        RateLimitExceededException refused =
                catchThrowableOfType(RateLimitExceededException.class, () -> chat(limiter, "10.0.0.3"));
        assertThat(refused.reason()).isEqualTo(Reason.DAILY_CAP);
        assertThat(refused.retryAfterSeconds()).isEqualTo(3600);   // one hour to midnight UTC

        clock.advance(Duration.ofHours(1));                         // 2026-11-05T00:00:00Z
        assertThat(chat(limiter, "10.0.0.3")).isTrue();
    }

    @Test
    void resetForgetsAllCounts() {
        RateLimitInterceptor limiter = new RateLimitInterceptor(new RateLimitProperties(1, 1));
        assertThat(chat(limiter, "10.0.0.1")).isTrue();

        limiter.reset();

        assertThat(chat(limiter, "10.0.0.1")).isTrue();
    }

    private static boolean chat(RateLimitInterceptor limiter, String ip) {
        return call(limiter, "POST", ip);
    }

    private static boolean call(RateLimitInterceptor limiter, String method, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/chat");
        request.setRemoteAddr(ip);
        return limiter.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    /** A clock the test can move forward. */
    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
