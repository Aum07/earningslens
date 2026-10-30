package com.earningslens.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * OpenAI model settings, read from the "earningslens.llm" section of application.yaml.
 *
 * JAVA CONCEPT, record: a record is a short way to write an immutable data class.
 * Java generates the constructor, getters (chatModel(), not getChatModel()),
 * equals, hashCode and toString for you. Once created, the values cannot change.
 *
 * SPRING CONCEPT, @ConfigurationProperties: Spring reads the YAML keys under the
 * given prefix and passes them into this record's constructor. "chat-model" in YAML
 * maps to chatModel here (Spring converts kebab-case to camelCase).
 *
 * @Validated plus the jakarta.validation annotations make the app refuse to start
 * if a value is missing or out of range, so a bad config fails fast and loudly.
 */
@Validated
@ConfigurationProperties(prefix = "earningslens.llm")
public record LlmProperties(
        @NotBlank String chatModel,
        @NotBlank String embeddingModel,
        @Positive int embeddingDimensions,
        @DecimalMin("0.0") @DecimalMax("2.0") double temperature,
        @Positive int maxOutputTokens
) {
}
