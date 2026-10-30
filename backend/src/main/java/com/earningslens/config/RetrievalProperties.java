package com.earningslens.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Retrieval settings from spec section 8, read from "earningslens.retrieval".
 *
 * minSimilarity is tuned later on the dev split only (task E3.7).
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.retrieval")
public record RetrievalProperties(
        @Positive int k,                    // chunks to return for a single-period question
        @Positive int kPerPeriod,           // chunks per period for multi-quarter questions
        @Positive int maxPeriods,           // most periods searched in one question
        @Positive int vectorCandidates,     // top N from vector search before fusion
        @Positive int keywordCandidates,    // top N from full-text search before fusion
        @Positive int rrfK,                 // the constant 60 in 1 / (60 + rank)
        @DecimalMin("0.0") @DecimalMax("1.0") double minSimilarity
) {
}
