package com.earningslens.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * SEC EDGAR download settings, read from "earningslens.sec".
 * The SEC requires a User-Agent with a name and contact email, and fewer than
 * 10 requests per second, so we wait between requests.
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.sec")
public record SecProperties(
        @NotBlank String userAgent,
        @Positive long minDelayMillis
) {
}
