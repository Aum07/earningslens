package com.earningslens.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Abuse and cost protection from spec section 13, read from "earningslens.rate-limit".
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.rate-limit")
public record RateLimitProperties(
        @Positive int requestsPerMinutePerIp,
        @Positive int dailyChatCap
) {
}
