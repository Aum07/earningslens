package com.earningslens.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Chunk sizes in tokens, read from "earningslens.chunking" (spec 7.2 step 4).
 *
 * @param targetTokens  a chunk is closed once adding more would pass this size
 * @param overlapTokens how much text from the end of a chunk is repeated at the start of the next
 * @param maxTokens     hard limit; no chunk is ever larger
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.chunking")
public record ChunkingProperties(
        @Positive int targetTokens,
        @PositiveOrZero int overlapTokens,
        @Positive int maxTokens
) {
}
