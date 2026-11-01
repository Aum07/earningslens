package com.earningslens.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Embedding call settings, read from "earningslens.embedding" (spec 7.2 step 5).
 * The model name and dimensions live in LlmProperties.
 *
 * @param batchSize            texts per API call (OpenAI allows up to 2048; the spec uses 64)
 * @param maxRetries           retries after a 429 or 5xx before giving up
 * @param initialBackoffMillis wait before the first retry; doubles each time
 * @param maxBackoffMillis     longest single wait
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.embedding")
public record EmbeddingProperties(
        @Positive @Max(2048) int batchSize,
        @PositiveOrZero int maxRetries,
        @Positive long initialBackoffMillis,
        @Positive long maxBackoffMillis
) {
}
